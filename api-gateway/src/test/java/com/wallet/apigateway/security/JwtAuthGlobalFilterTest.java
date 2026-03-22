package com.wallet.apigateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthGlobalFilterTest {

    private JwtTokenValidator jwtTokenValidator;
    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenValidator = mock(JwtTokenValidator.class);
        filter = new JwtAuthGlobalFilter(jwtTokenValidator);
    }

    @Test
    void filter_withPublicPath_skipsAuthentication() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/swagger-ui/index.html").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_withoutAuthorization_returnsUnauthorized() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/wallet/balance").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_withNonAdminOnAdminPath_returnsForbidden() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("userId")).thenReturn(22L);
        when(claims.getSubject()).thenReturn("bob");

        when(jwtTokenValidator.validateAndGetClaims("valid-token")).thenReturn(claims);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void filter_withValidToken_mutatesHeadersAndContinues() {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            captured.set(exchange);
            return Mono.empty();
        };

        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("userId")).thenReturn(44L);
        when(claims.getSubject()).thenReturn("charlie");

        when(jwtTokenValidator.validateAndGetClaims("ok-token")).thenReturn(claims);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/wallet/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ok-token")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-Authenticated-User")).isEqualTo("charlie");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-Authenticated-Role")).isEqualTo("USER");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-Authenticated-UserId")).isEqualTo("44");
    }
}



