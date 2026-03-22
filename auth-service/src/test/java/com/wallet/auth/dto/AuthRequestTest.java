package com.wallet.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestTest {

    @Test
    void recordFields_areAccessible() {
        AuthRequest request = new AuthRequest("alice", "secret", "USER");

        assertThat(request.username()).isEqualTo("alice");
        assertThat(request.password()).isEqualTo("secret");
        assertThat(request.role()).isEqualTo("USER");
    }
}

