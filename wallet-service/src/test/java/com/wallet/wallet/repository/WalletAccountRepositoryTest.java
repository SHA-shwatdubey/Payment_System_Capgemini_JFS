package com.wallet.wallet.repository;

import com.wallet.wallet.entity.WalletAccount;
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
class WalletAccountRepositoryTest {

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Test
    void findByUserId_returnsSavedAccount() {
        WalletAccount account = new WalletAccount();
        account.setUserId(55L);
        account.setBalance(new BigDecimal("1200.00"));
        walletAccountRepository.save(account);

        assertThat(walletAccountRepository.findByUserId(55L))
                .isPresent()
                .get()
                .extracting(WalletAccount::getBalance)
                .isEqualTo(new BigDecimal("1200.00"));
    }
}




