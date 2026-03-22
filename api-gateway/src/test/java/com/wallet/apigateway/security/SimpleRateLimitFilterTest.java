package com.wallet.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleRateLimitFilterTest {

    @Test
    void filter_excludedPath_allowsRequest() {
        SimpleRateLimitFilter filter = new SimpleRateLimitFilter(1);
        AtomicInteger chainCount = new AtomicInteger(0);
        GatewayFilterChain chain = exchange -> {
            chainCount.incrementAndGet();
            return Mono.empty();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        filter.filter(exchange, chain).block();

        assertThat(chainCount.get()).isEqualTo(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_whenLimitExceeded_returns429() {
        SimpleRateLimitFilter filter = new SimpleRateLimitFilter(1);
        GatewayFilterChain chain = exchange -> Mono.empty();

        MockServerHttpRequest firstReq = MockServerHttpRequest.get("/api/wallet/balance")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 7777))
                .build();
        MockServerHttpRequest secondReq = MockServerHttpRequest.get("/api/wallet/balance")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 7777))
                .build();

        MockServerWebExchange first = MockServerWebExchange.from(firstReq);
        MockServerWebExchange second = MockServerWebExchange.from(secondReq);

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();

        assertThat(first.getResponse().getStatusCode()).isNull();
        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }
}

