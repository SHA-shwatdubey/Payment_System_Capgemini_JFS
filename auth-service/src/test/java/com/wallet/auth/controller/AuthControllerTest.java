package com.wallet.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.repository.AuthUserRepository;
import com.wallet.auth.service.AuthService;
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

@WebMvcTest(value = AuthController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthUserRepository authUserRepository;

    @Test
    void signup_returnsCreated() throws Exception {
        AuthRequest request = new AuthRequest("alice", "pass", "USER");
        when(authService.signup(any(AuthRequest.class)))
                .thenReturn(new AuthResponse(null, "USER", "Signup successful. Please login to get token"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_returnsToken() throws Exception {
        AuthRequest request = new AuthRequest("alice", "pass", null);
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "USER", "Login successful"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void signup_whenServiceThrowsIllegalArgument_returnsBadRequest() throws Exception {
        AuthRequest request = new AuthRequest("alice", "pass", "USER");
        when(authService.signup(any(AuthRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void testEndpoint_returnsHealthPayload() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Authentication Service"));
    }

    @Test
    void testUserEndpoint_returnsSeedUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/test/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shashwat"))
                .andExpect(jsonPath("$.status").value("✅ Ready for OTP Testing"));
    }
}







