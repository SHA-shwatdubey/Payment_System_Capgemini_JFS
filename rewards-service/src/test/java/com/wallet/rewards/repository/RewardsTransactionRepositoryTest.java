package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardsTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

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
class RewardsTransactionRepositoryTest {

    @Autowired
    private RewardsTransactionRepository rewardsTransactionRepository;

    @Test
    void save_persistsTransaction() {
        RewardsTransaction tx = new RewardsTransaction();
        tx.setUserId(9L);
        tx.setPoints(15);
        tx.setType("EARN_TOPUP");
        tx.setReference("REF-9");
        tx.setCreatedAt(LocalDateTime.now());

        RewardsTransaction saved = rewardsTransactionRepository.save(tx);

        assertThat(saved.getId()).isNotNull();
        assertThat(rewardsTransactionRepository.findById(saved.getId())).isPresent();
    }
}

