package com.wallet.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transaction.dto.DisputeCreateRequest;
import com.wallet.transaction.dto.DisputeResolveRequest;
import com.wallet.transaction.entity.Dispute;
import com.wallet.transaction.service.DisputeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DisputeController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisputeService disputeService;

    @Test
    void create_returnsCreatedDispute() throws Exception {
        Dispute dispute = new Dispute();
        dispute.setId(5L);
        dispute.setStatus("OPEN");
        when(disputeService.create(any(DisputeCreateRequest.class))).thenReturn(dispute);

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisputeCreateRequest(10L, 4L, "wrong amount"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void openDisputes_returnsArray() throws Exception {
        Dispute dispute = new Dispute();
        dispute.setId(9L);
        when(disputeService.openDisputes()).thenReturn(List.of(dispute));

        mockMvc.perform(get("/api/support/disputes/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9L));
    }

    @Test
    void resolve_returnsResolvedDispute() throws Exception {
        Dispute dispute = new Dispute();
        dispute.setId(12L);
        dispute.setStatus("RESOLVED");
        when(disputeService.resolve(any(Long.class), any(DisputeResolveRequest.class))).thenReturn(dispute);

        mockMvc.perform(put("/api/disputes/12/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisputeResolveRequest("closed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}

