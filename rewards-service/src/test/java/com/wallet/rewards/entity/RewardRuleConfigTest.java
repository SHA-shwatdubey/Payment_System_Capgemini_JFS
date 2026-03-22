package com.wallet.rewards.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewardRuleConfigTest {

    @Test
    void gettersAndSetters_work() {
        RewardRuleConfig config = new RewardRuleConfig();
        config.setId(1L);
        config.setPointsPer100(10);
        config.setGoldThreshold(1000);
        config.setPlatinumThreshold(5000);

        assertThat(config.getId()).isEqualTo(1L);
        assertThat(config.getPointsPer100()).isEqualTo(10);
        assertThat(config.getGoldThreshold()).isEqualTo(1000);
        assertThat(config.getPlatinumThreshold()).isEqualTo(5000);
    }
}

