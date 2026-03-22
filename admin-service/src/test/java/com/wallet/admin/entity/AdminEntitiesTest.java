package com.wallet.admin.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEntitiesTest {

    @Test
    void campaign_gettersAndSetters_work() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Festive");
        campaign.setRuleType("TOPUP");
        campaign.setBonusPoints(100);
        campaign.setStartDate(LocalDate.now());
        campaign.setEndDate(LocalDate.now().plusDays(10));

        assertThat(campaign.getId()).isEqualTo(1L);
        assertThat(campaign.getName()).isEqualTo("Festive");
        assertThat(campaign.getRuleType()).isEqualTo("TOPUP");
        assertThat(campaign.getBonusPoints()).isEqualTo(100);
    }

    @Test
    void adminAction_gettersAndSetters_work() {
        AdminAction action = new AdminAction();
        LocalDateTime now = LocalDateTime.now();
        action.setId(2L);
        action.setActionType("APPROVE_KYC");
        action.setTargetId(20L);
        action.setStatus("SUCCESS");
        action.setReason("ok");
        action.setCreatedAt(now);

        assertThat(action.getId()).isEqualTo(2L);
        assertThat(action.getActionType()).isEqualTo("APPROVE_KYC");
        assertThat(action.getTargetId()).isEqualTo(20L);
        assertThat(action.getStatus()).isEqualTo("SUCCESS");
        assertThat(action.getReason()).isEqualTo("ok");
        assertThat(action.getCreatedAt()).isEqualTo(now);
    }
}

