package com.wallet.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpVerificationDtoTest {

    @Test
    void constructor_setsAllFields() {
        OtpVerificationDto dto = new OtpVerificationDto("john@example.com", "9876543210", "123456");

        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(dto.getOtp()).isEqualTo("123456");
    }

    @Test
    void setters_updateFields() {
        OtpVerificationDto dto = new OtpVerificationDto();

        dto.setEmail("new@example.com");
        dto.setPhoneNumber("9999999999");
        dto.setOtp("999888");

        assertThat(dto.getEmail()).isEqualTo("new@example.com");
        assertThat(dto.getPhoneNumber()).isEqualTo("9999999999");
        assertThat(dto.getOtp()).isEqualTo("999888");
    }
}

