package com.wallet.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void recordFields_areAccessible() {
        AuthResponse response = new AuthResponse("token-123", "MERCHANT", "Login successful");

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.role()).isEqualTo("MERCHANT");
        assertThat(response.message()).isEqualTo("Login successful");
    }
}

