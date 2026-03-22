package com.wallet.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OtpEntityTest {

    @Test
    void constructor_initializesDefaults() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        OtpEntity entity = new OtpEntity("john@example.com", "9876543210", "123456", "EMAIL", expiresAt);

        assertThat(entity.getEmail()).isEqualTo("john@example.com");
        assertThat(entity.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(entity.getOtp()).isEqualTo("123456");
        assertThat(entity.getOtpType()).isEqualTo("EMAIL");
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.getGeneratedAt()).isNotNull();
        assertThat(entity.getIsUsed()).isFalse();
        assertThat(entity.getIsVerified()).isFalse();
        assertThat(entity.getAttemptCount()).isZero();
    }

    @Test
    void setters_updateFields() {
        OtpEntity entity = new OtpEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(10L);
        entity.setEmail("new@example.com");
        entity.setPhoneNumber("9999999999");
        entity.setOtp("987654");
        entity.setOtpType("SMS");
        entity.setGeneratedAt(now);
        entity.setExpiresAt(now.plusMinutes(1));
        entity.setIsUsed(true);
        entity.setIsVerified(true);
        entity.setAttemptCount(3);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getPhoneNumber()).isEqualTo("9999999999");
        assertThat(entity.getOtp()).isEqualTo("987654");
        assertThat(entity.getOtpType()).isEqualTo("SMS");
        assertThat(entity.getGeneratedAt()).isEqualTo(now);
        assertThat(entity.getExpiresAt()).isEqualTo(now.plusMinutes(1));
        assertThat(entity.getIsUsed()).isTrue();
        assertThat(entity.getIsVerified()).isTrue();
        assertThat(entity.getAttemptCount()).isEqualTo(3);
    }
}

