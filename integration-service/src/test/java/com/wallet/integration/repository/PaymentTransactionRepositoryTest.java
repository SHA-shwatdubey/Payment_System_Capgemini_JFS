package com.wallet.integration.repository;

import com.wallet.integration.entity.PaymentTransaction;
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
class PaymentTransactionRepositoryTest {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void findByPaymentRef_returnsEntity() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentRef("PAY-XYZ");
        tx.setUserId(3L);
        tx.setAmount(new BigDecimal("99.99"));
        tx.setMethod("CARD");
        tx.setStatus("SUCCESS");
        paymentTransactionRepository.save(tx);

        assertThat(paymentTransactionRepository.findByPaymentRef("PAY-XYZ")).isPresent();
    }
}




