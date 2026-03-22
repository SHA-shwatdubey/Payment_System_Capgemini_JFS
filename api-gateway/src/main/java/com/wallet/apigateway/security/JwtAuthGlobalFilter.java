package com.wallet.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPPORT = "SUPPORT";
    private static final String ROLE_MERCHANT = "MERCHANT";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/auth",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthGlobalFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        final Claims claims;
        try {
            claims = jwtTokenValidator.validateAndGetClaims(token);
        } catch (JwtException ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String role = normalizeRole(claims.get("role", String.class));
        String userId = claims.get("userId") == null ? "" : String.valueOf(claims.get("userId"));
        String username = claims.getSubject() == null ? "" : claims.getSubject();

        if (!isAuthorized(path, role)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "Access denied for role: " + role);
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Authenticated-User", username)
                .header("X-Authenticated-Role", role)
                .header("X-Authenticated-UserId", userId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }

        if ("/".equals(path)) {
            return true;
        }

        return PUBLIC_PATH_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private boolean isAuthorized(String path, String role) {
        boolean adminOnly = path.startsWith("/api/admin/")
                || path.equals("/api/campaigns")
                || path.startsWith("/api/campaigns/")
                || path.equals("/api/kyc/pending")
                || path.matches("^/api/kyc/[^/]+/status$")
                || path.startsWith("/api/wallet/admin/")
                || path.startsWith("/api/rewards/admin/")
                || path.startsWith("/api/notifications/admin/")
                || path.matches("^/api/disputes/[^/]+/resolve$");

        boolean supportOrAdmin = path.startsWith("/api/support/")
                || path.matches("^/api/disputes/[^/]+/escalate$");

        boolean merchantOrAdmin = path.startsWith("/api/merchant/")
                || path.startsWith("/api/rewards/merchant/");

        if (adminOnly) {
            return ROLE_ADMIN.equals(role);
        }

        if (supportOrAdmin) {
            return ROLE_ADMIN.equals(role) || ROLE_SUPPORT.equals(role);
        }

        if (merchantOrAdmin) {
            return ROLE_ADMIN.equals(role) || ROLE_MERCHANT.equals(role);
        }

        return role != null && !role.isBlank();
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
