package com.wallet.rewards.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void dtoRecords_coverBasicContract() {
        WalletEvent walletEvent = new WalletEvent(1L, "TOPUP", new BigDecimal("100"));
        NotificationEvent notificationEvent = new NotificationEvent(1L, "POINTS_EARNED", "EMAIL", "a@b.com", "done");
        RedeemRequest redeemRequest = new RedeemRequest(2L, 7L);
        MerchantItemRequest merchantItemRequest = new MerchantItemRequest(9L, "Voucher", 100, 10, "GIFT");
        RewardRuleUpdateRequest ruleUpdateRequest = new RewardRuleUpdateRequest(10, 1000, 5000);

        assertThat(walletEvent.eventType()).isEqualTo("TOPUP");
        assertThat(notificationEvent.channel()).isEqualTo("EMAIL");
        assertThat(redeemRequest.rewardId()).isEqualTo(7L);
        assertThat(merchantItemRequest.name()).isEqualTo("Voucher");
        assertThat(ruleUpdateRequest.goldThreshold()).isEqualTo(1000);
    }
}

