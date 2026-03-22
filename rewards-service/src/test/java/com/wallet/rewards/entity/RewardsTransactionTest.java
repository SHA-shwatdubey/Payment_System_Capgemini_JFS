package com.wallet.rewards.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RewardsTransactionTest {

    @Test
    void gettersAndSetters_work() {
        RewardsTransaction tx = new RewardsTransaction();
        LocalDateTime now = LocalDateTime.now();
        tx.setId(2L);
        tx.setUserId(15L);
        tx.setPoints(50);
        tx.setType("EARN_TOPUP");
        tx.setReference("REF-1");
        tx.setCreatedAt(now);

        assertThat(tx.getId()).isEqualTo(2L);
        assertThat(tx.getUserId()).isEqualTo(15L);
        assertThat(tx.getPoints()).isEqualTo(50);
        assertThat(tx.getType()).isEqualTo("EARN_TOPUP");
        assertThat(tx.getReference()).isEqualTo("REF-1");
        assertThat(tx.getCreatedAt()).isEqualTo(now);
    }
}

