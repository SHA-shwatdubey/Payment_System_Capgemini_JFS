package com.wallet.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.admin.dto.CampaignRequest;
import com.wallet.admin.dto.KycApprovalRequest;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Test
    void createCampaign_returnsCreatedCampaign() throws Exception {
        CampaignRequest request = new CampaignRequest("Fest", "TOPUP", 50, LocalDate.now(), LocalDate.now().plusDays(7));
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setName("Fest");

        when(adminService.createCampaign(any(CampaignRequest.class))).thenReturn(campaign);

        mockMvc.perform(post("/api/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Fest"));
    }

    @Test
    void approveKyc_returnsClientResponse() throws Exception {
        KycApprovalRequest request = new KycApprovalRequest("APPROVED", "verified");
        when(adminService.approveKyc(eq(99L), any(KycApprovalRequest.class))).thenReturn(Map.of("status", "APPROVED"));

        mockMvc.perform(post("/api/admin/kyc/99/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void pendingKyc_returnsList() throws Exception {
        List<Object> pending = new ArrayList<>();
        pending.add(Map.of("userId", 1L));
        when(adminService.pendingKyc()).thenReturn(pending);

        mockMvc.perform(get("/api/admin/kyc/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    @Test
    void dashboard_returnsAggregatedValues() throws Exception {
        Map<String, Object> dashboard = Map.of(
                "totalCampaigns", 4,
                "totalAdminActions", 9,
                "pendingKyc", 2
        );
        when(adminService.dashboard()).thenReturn(dashboard);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCampaigns").value(4))
                .andExpect(jsonPath("$.totalAdminActions").value(9))
                .andExpect(jsonPath("$.pendingKyc").value(2));
    }
}







