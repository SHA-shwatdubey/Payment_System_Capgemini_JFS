package com.wallet.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.auth.dto.OtpRequestDto;
import com.wallet.auth.dto.OtpVerificationDto;
import com.wallet.auth.repository.AuthUserRepository;
import com.wallet.auth.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OtpController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @MockBean
    private AuthUserRepository authUserRepository;

    @Test
    void generateOtp_whenTypeMissing_returnsBadRequest() throws Exception {
        OtpRequestDto request = new OtpRequestDto("john@example.com", null, null);

        mockMvc.perform(post("/api/auth/otp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP type is required. Use EMAIL or SMS"));
    }

    @Test
    void generateOtp_whenValid_returnsOk() throws Exception {
        OtpRequestDto request = new OtpRequestDto("john@example.com", null, "EMAIL");
        when(otpService.generateAndSendOtp(any(OtpRequestDto.class))).thenReturn("OTP sent to email: j**n@example.com");

        mockMvc.perform(post("/api/auth/otp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").value("OTP sent successfully. Valid for 5 minutes."));
    }

    @Test
    void verifyOtp_whenOtpMissing_returnsBadRequest() throws Exception {
        OtpVerificationDto verification = new OtpVerificationDto("john@example.com", null, null);

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verification)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void verifyOtp_whenValid_returnsIdentifierAndType() throws Exception {
        OtpVerificationDto verification = new OtpVerificationDto("john@example.com", null, "123456");
        when(otpService.verifyOtp(any(OtpVerificationDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.identifier").value("john@example.com"))
                .andExpect(jsonPath("$.type").value("EMAIL"));
    }

    @Test
    void verifyOtp_whenRuntimeError_returnsUnauthorized() throws Exception {
        OtpVerificationDto verification = new OtpVerificationDto("john@example.com", null, "123456");
        when(otpService.verifyOtp(any(OtpVerificationDto.class))).thenThrow(new RuntimeException("Invalid OTP"));

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verification)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void health_returnsOk() throws Exception {
        mockMvc.perform(get("/api/auth/otp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP Service is running"));
    }
}


