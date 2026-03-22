package com.wallet.wallet.repository;

import com.wallet.wallet.entity.WalletLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

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
class WalletLimitConfigRepositoryTest {

    @Autowired
    private WalletLimitConfigRepository walletLimitConfigRepository;

    @Test
    void saveAndFindById_returnsPersistedConfig() {
        WalletLimitConfig config = new WalletLimitConfig();
        config.setId(1L);
        config.setDailyTopupLimit(new BigDecimal("50000.00"));
        config.setDailyTransferLimit(new BigDecimal("25000.00"));
        config.setDailyTransferCountLimit(10);
        walletLimitConfigRepository.save(config);

        assertThat(walletLimitConfigRepository.findById(1L)).isPresent();
    }
}

