package com.wallet.admin.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDtoRecordsTest {

    @Test
    void dtoRecords_coverRecordContract() {
        CampaignRequest campaignRequest = new CampaignRequest("Campaign-1", "TOPUP", 50, LocalDate.now(), LocalDate.now().plusDays(5));
        KycApprovalRequest kycApprovalRequest = new KycApprovalRequest("APPROVED", "verified");

        assertThat(campaignRequest.name()).isEqualTo("Campaign-1");
        assertThat(campaignRequest.ruleType()).isEqualTo("TOPUP");
        assertThat(kycApprovalRequest.status()).isEqualTo("APPROVED");
        assertThat(kycApprovalRequest.reason()).isEqualTo("verified");
    }
}

