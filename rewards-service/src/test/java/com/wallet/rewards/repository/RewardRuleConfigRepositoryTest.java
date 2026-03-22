package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardRuleConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class RewardRuleConfigRepositoryTest {

    @Autowired
    private RewardRuleConfigRepository rewardRuleConfigRepository;

    @Test
    void save_persistsRuleConfig() {
        RewardRuleConfig config = new RewardRuleConfig();
        config.setId(1L);
        config.setPointsPer100(10);
        config.setGoldThreshold(1000);
        config.setPlatinumThreshold(5000);

        RewardRuleConfig saved = rewardRuleConfigRepository.save(config);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(rewardRuleConfigRepository.findById(1L)).isPresent();
    }
}

