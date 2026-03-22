package com.wallet.transaction.repository;

import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
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
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByIdempotencyKey_returnsSavedTransaction() {
        Transaction transaction = new Transaction();
        transaction.setUserId(8L);
        transaction.setSenderId(0L);
        transaction.setReceiverId(8L);
        transaction.setAmount(new BigDecimal("75.00"));
        transaction.setType(TransactionType.TOPUP);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey("repo-key-1");
        transactionRepository.save(transaction);

        assertThat(transactionRepository.findByIdempotencyKey("repo-key-1")).isPresent();
    }
}




