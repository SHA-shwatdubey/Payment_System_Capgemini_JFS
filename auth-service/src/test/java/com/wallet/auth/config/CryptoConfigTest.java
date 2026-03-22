package com.wallet.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoConfigTest {

    @Test
    void passwordEncoder_returnsBcryptEncoder() {
        CryptoConfig config = new CryptoConfig();

        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("secret");

        assertThat(encoded).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", encoded)).isTrue();
    }
}

