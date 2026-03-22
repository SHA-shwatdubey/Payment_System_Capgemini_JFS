package com.wallet.rewards.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewardsAccountTest {

    @Test
    void gettersAndSetters_work() {
        RewardsAccount account = new RewardsAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setPoints(120);
        account.setTier("GOLD");

        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getUserId()).isEqualTo(10L);
        assertThat(account.getPoints()).isEqualTo(120);
        assertThat(account.getTier()).isEqualTo("GOLD");
    }
}

