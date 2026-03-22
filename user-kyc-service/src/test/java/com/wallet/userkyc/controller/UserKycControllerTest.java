package com.wallet.userkyc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.userkyc.dto.KycStatusRequest;
import com.wallet.userkyc.entity.UserProfile;
import com.wallet.userkyc.security.JwtRoleValidator;
import com.wallet.userkyc.service.UserKycService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserKycController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class UserKycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserKycService userKycService;

    @MockBean
    private JwtRoleValidator jwtRoleValidator;

    @Test
    void createUser_returnsCreated() throws Exception {
        UserProfile request = new UserProfile();
        request.setFullName("Alice");
        request.setEmail("alice@example.com");

        UserProfile response = new UserProfile();
        response.setId(1L);
        response.setFullName("Alice");

        when(userKycService.createUser(any(UserProfile.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Alice"));
    }

    @Test
    void getUser_returnsUser() throws Exception {
        UserProfile response = new UserProfile();
        response.setId(2L);
        response.setAuthUserId(2L);

        when(userKycService.getUser(2L)).thenReturn(response);

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    void getUser_withInvalidId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void supportGetUser_returnsUser() throws Exception {
        UserProfile response = new UserProfile();
        response.setId(3L);

        when(userKycService.getUser(3L)).thenReturn(response);

        mockMvc.perform(get("/api/support/users/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));
    }

    @Test
    void submitKyc_returnsUpdatedProfile() throws Exception {
        UserProfile response = new UserProfile();
        response.setId(5L);
        response.setKycDocumentId("DOC-123");

        when(userKycService.submitKyc(5L, "DOC-123")).thenReturn(response);

        mockMvc.perform(post("/api/kyc/submit/5").param("documentId", "DOC-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycDocumentId").value("DOC-123"));
    }

    @Test
    void submitKycWithFile_returnsUpdatedProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "document",
                "doc.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        UserProfile response = new UserProfile();
        response.setId(7L);
        response.setKycStatus("PENDING");

        when(userKycService.submitKycFile(eq(7L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/kyc/upload/7").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    void updateStatus_withAdminHeader_allowsUpdate() throws Exception {
        UserProfile response = new UserProfile();
        response.setId(10L);
        response.setKycStatus("APPROVED");

        when(userKycService.updateKycStatus(eq(10L), any(KycStatusRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/kyc/10/status")
                        .header("X-Authenticated-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KycStatusRequest("APPROVED", "ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"));
    }

    @Test
    void updateStatus_withNonAdminToken_returnsForbidden() throws Exception {
        when(jwtRoleValidator.extractRole("bad-token")).thenThrow(new JwtException("invalid"));

        mockMvc.perform(put("/api/kyc/10/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KycStatusRequest("APPROVED", "ok"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingKyc_withBearerAdmin_returnsList() throws Exception {
        UserProfile profile = new UserProfile();
        profile.setId(22L);
        when(jwtRoleValidator.extractRole("token-admin")).thenReturn("ADMIN");
        when(userKycService.pendingKyc()).thenReturn(List.of(profile));

        mockMvc.perform(get("/api/kyc/pending").header(HttpHeaders.AUTHORIZATION, "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(22L));

        verify(jwtRoleValidator).extractRole("token-admin");
    }
}

