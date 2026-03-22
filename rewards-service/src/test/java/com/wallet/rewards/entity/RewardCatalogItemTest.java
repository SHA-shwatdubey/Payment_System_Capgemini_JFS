package com.wallet.rewards.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewardCatalogItemTest {

    @Test
    void gettersAndSetters_work() {
        RewardCatalogItem item = new RewardCatalogItem();
        item.setId(3L);
        item.setName("Voucher");
        item.setPointsCost(120);
        item.setStock(50);
        item.setRewardType("GIFT");
        item.setMerchantId(9L);
        item.setRedeemCount(4);

        assertThat(item.getId()).isEqualTo(3L);
        assertThat(item.getName()).isEqualTo("Voucher");
        assertThat(item.getPointsCost()).isEqualTo(120);
        assertThat(item.getStock()).isEqualTo(50);
        assertThat(item.getRewardType()).isEqualTo("GIFT");
        assertThat(item.getMerchantId()).isEqualTo(9L);
        assertThat(item.getRedeemCount()).isEqualTo(4);
    }
}

