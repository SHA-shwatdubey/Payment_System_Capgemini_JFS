package com.wallet.rewards.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.rewards.dto.RedeemRequest;
import com.wallet.rewards.entity.RewardsAccount;
import com.wallet.rewards.service.RewardsService;
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

@WebMvcTest(value = RewardsController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class RewardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RewardsService rewardsService;

    @Test
    void summary_returnsRewardsAccount() throws Exception {
        RewardsAccount account = new RewardsAccount();
        account.setUserId(4L);
        account.setPoints(230);
        account.setTier("SILVER");
        when(rewardsService.summary(4L)).thenReturn(account);

        mockMvc.perform(get("/api/rewards/summary").param("userId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(230));
    }

    @Test
    void redeem_returnsSuccessMessage() throws Exception {
        when(rewardsService.redeem(any(RedeemRequest.class))).thenReturn("Redemption successful");

        mockMvc.perform(post("/api/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemRequest(4L, 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Redemption successful"));
    }

    @Test
    void catalog_returnsArray() throws Exception {
        when(rewardsService.catalog()).thenReturn(List.of());

        mockMvc.perform(get("/api/rewards/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}



