package com.wallet.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.entity.NotificationChannel;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationStatus;
import com.wallet.notification.security.JwtRoleValidator;
import com.wallet.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NotificationController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtRoleValidator jwtRoleValidator;

    @Test
    void send_withInternalHeader_returnsCreated() throws Exception {
        NotificationMessage message = new NotificationMessage();
        message.setId(1L);
        message.setUserId(10L);
        message.setEventType("KYC_APPROVED");
        message.setChannel(NotificationChannel.EMAIL);
        message.setTarget("user-10");
        message.setMessage("approved");
        message.setStatus(NotificationStatus.SENT);
        when(notificationService.send(any(NotificationSendRequest.class))).thenReturn(message);

        NotificationSendRequest request = new NotificationSendRequest(10L, "KYC_APPROVED", NotificationChannel.EMAIL, "user-10", "approved");
        mockMvc.perform(post("/api/notifications/send")
                        .header("X-Internal-Call", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void history_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications/history").param("userId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void history_withInternalHeader_returnsList() throws Exception {
        when(notificationService.history(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/history")
                        .header("X-Internal-Call", "true")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}



