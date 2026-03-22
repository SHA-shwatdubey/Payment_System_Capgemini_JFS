package com.wallet.wallet.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WalletExceptionHandlerTest {

    private final WalletExceptionHandler handler = new WalletExceptionHandler();

    @Test
    void handleBadRequest_returns400Payload() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid amount"), request("/api/wallet/topup")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("invalid amount");
    }

    @Test
    void handleConflict_returns409Payload() {
        ResponseEntity<Map<String, Object>> response = handler.handleConflict(
                new IllegalStateException("duplicate"), request("/api/wallet/transfer")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("path")).isEqualTo("/api/wallet/transfer");
    }

    @Test
    void handleUnexpected_returns500Payload() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(
                new RuntimeException("db down"), request("/api/wallet/transactions")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Internal server error");
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}

