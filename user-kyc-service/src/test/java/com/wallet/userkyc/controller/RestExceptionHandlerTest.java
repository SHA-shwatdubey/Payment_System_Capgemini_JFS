package com.wallet.userkyc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void handleNotFound_returns404Payload() {
        Map<String, Object> body = handler.handleNotFound(new NoSuchElementException("missing")).getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.get("message")).isEqualTo("Resource not found");
    }

    @Test
    void handleIllegalArgument_returns400Payload() {
        Map<String, Object> body = handler.handleIllegalArgument(new IllegalArgumentException("bad input")).getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.get("message")).isEqualTo("bad input");
    }

    @Test
    void handleFileTooLarge_returnsFriendlyMessage() {
        Map<String, Object> body = handler.handleFileTooLarge(new MaxUploadSizeExceededException(1L)).getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.get("message")).isEqualTo("Uploaded file is too large");
    }
}

