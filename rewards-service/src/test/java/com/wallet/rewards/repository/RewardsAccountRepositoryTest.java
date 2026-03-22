package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardsAccount;
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
class RewardsAccountRepositoryTest {

    @Autowired
    private RewardsAccountRepository rewardsAccountRepository;

    @Test
    void findByUserId_returnsSavedAccount() {
        RewardsAccount account = new RewardsAccount();
        account.setUserId(50L);
        account.setPoints(400);
        account.setTier("SILVER");
        rewardsAccountRepository.save(account);

        assertThat(rewardsAccountRepository.findByUserId(50L)).isPresent();
    }
}

