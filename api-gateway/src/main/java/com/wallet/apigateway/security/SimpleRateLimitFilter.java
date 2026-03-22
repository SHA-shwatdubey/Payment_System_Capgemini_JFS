package com.wallet.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimitFilter implements GlobalFilter, Ordered {

    private static final long ONE_MINUTE_SECONDS = 60L;

    private final int requestsPerMinute;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public SimpleRateLimitFilter(@Value("${gateway.rate-limit.requests-per-minute:120}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public int getOrder() {
        return -90;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        String principalKey = exchange.getRequest().getHeaders().getFirst("X-Authenticated-User");
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String fallbackIp = (remoteAddress == null || remoteAddress.getAddress() == null)
                ? "unknown"
                : remoteAddress.getAddress().getHostAddress();
        String key = (principalKey == null || principalKey.isBlank()) ? fallbackIp : principalKey;
        long nowSeconds = Instant.now().getEpochSecond();
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(nowSeconds, new AtomicInteger(0)));

        synchronized (counter) {
            if (nowSeconds - counter.windowStart >= ONE_MINUTE_SECONDS) {
                counter.windowStart = nowSeconds;
                counter.requestCount.set(0);
            }

            int current = counter.requestCount.incrementAndGet();
            if (current > requestsPerMinute) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                exchange.getResponse().getHeaders().add("Retry-After", "60");
                String body = "{\"status\":429,\"message\":\"Rate limit exceeded. Try again after 60 seconds\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }
        }

        return chain.filter(exchange);
    }

    private boolean isExcludedPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return true;
        }
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator");
    }

    private static final class WindowCounter {
        private long windowStart;
        private final AtomicInteger requestCount;

        private WindowCounter(long windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}


