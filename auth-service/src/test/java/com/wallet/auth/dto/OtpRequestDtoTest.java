package com.wallet.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpRequestDtoTest {

    @Test
    void constructor_setsAllFields() {
        OtpRequestDto dto = new OtpRequestDto("john@example.com", "9876543210", "EMAIL");

        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(dto.getOtpType()).isEqualTo("EMAIL");
    }

    @Test
    void setters_updateFields() {
        OtpRequestDto dto = new OtpRequestDto();

        dto.setEmail("new@example.com");
        dto.setPhoneNumber("9999999999");
        dto.setOtpType("SMS");

        assertThat(dto.getEmail()).isEqualTo("new@example.com");
        assertThat(dto.getPhoneNumber()).isEqualTo("9999999999");
        assertThat(dto.getOtpType()).isEqualTo("SMS");
    }
}

