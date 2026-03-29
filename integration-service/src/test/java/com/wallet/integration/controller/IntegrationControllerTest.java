package com.wallet.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.integration.dto.PaymentInitRequest;
import com.wallet.integration.dto.PaymentInitResponse;
import com.wallet.integration.security.JwtRoleValidator;
import com.wallet.integration.service.IntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = IntegrationController.class, properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.config.fail-fast=false",
                "spring.config.import=optional:configserver:",
                "eureka.client.enabled=false"
})
class IntegrationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private IntegrationService integrationService;

        @MockBean
        private JwtRoleValidator jwtRoleValidator;

        @Test
        void initPayment_withInternalHeader_returnsResponse() throws Exception {
                when(integrationService.initPayment(any(PaymentInitRequest.class)))
                                .thenReturn(new PaymentInitResponse("PAY-1", "PENDING", "https://mock/1"));

                PaymentInitRequest request = new PaymentInitRequest(9L, new BigDecimal("200"), "UPI");
                mockMvc.perform(post("/api/integrations/payments/init")
                                .header("X-Internal-Call", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentRef").value("PAY-1"));
        }

        @Test
        void initPayment_withoutAuth_returnsUnauthorized() throws Exception {
                PaymentInitRequest request = new PaymentInitRequest(9L, new BigDecimal("200"), "UPI");

                mockMvc.perform(post("/api/integrations/payments/init")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }
}
