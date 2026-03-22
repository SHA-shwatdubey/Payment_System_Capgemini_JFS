package com.wallet.transaction.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_mapsTo400() {
        HttpServletRequest request = request("/transactions/topup");

        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new IllegalArgumentException("bad payload"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("bad payload");
        assertThat(response.getBody().path()).isEqualTo("/transactions/topup");
    }

    @Test
    void handleConflict_mapsIdempotencyErrorTo409() {
        HttpServletRequest request = request("/transactions/transfer");

        ResponseEntity<ApiError> response = handler.handleConflict(
                new IdempotencyConflictException("already used"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("already used");
    }

    @Test
    void handleGeneric_mapsTo500() {
        HttpServletRequest request = request("/transactions/statement");

        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("oops"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("oops");
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}

