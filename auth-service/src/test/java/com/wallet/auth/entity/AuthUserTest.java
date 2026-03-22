package com.wallet.auth.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUserTest {

    @Test
    void allArgsConstructor_setsFields() {
        AuthUser user = new AuthUser(1L, "alice", "pwd", "USER");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("pwd");
        assertThat(user.getRole()).isEqualTo("USER");
    }

    @Test
    void setters_updateFields() {
        AuthUser user = new AuthUser();

        user.setId(2L);
        user.setUsername("bob");
        user.setPassword("enc");
        user.setRole("MERCHANT");

        assertThat(user.getId()).isEqualTo(2L);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPassword()).isEqualTo("enc");
        assertThat(user.getRole()).isEqualTo("MERCHANT");
    }
}

