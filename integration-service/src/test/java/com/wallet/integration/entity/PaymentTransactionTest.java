package com.wallet.integration.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTransactionTest {

    @Test
    void gettersSettersAndPrePersist_workAsExpected() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(1L);
        tx.setPaymentRef("PAY-1");
        tx.setUserId(11L);
        tx.setAmount(new BigDecimal("99.99"));
        tx.setMethod("CARD");
        tx.setStatus("PENDING");

        tx.onCreate();

        assertThat(tx.getId()).isEqualTo(1L);
        assertThat(tx.getPaymentRef()).isEqualTo("PAY-1");
        assertThat(tx.getUserId()).isEqualTo(11L);
        assertThat(tx.getAmount()).isEqualByComparingTo("99.99");
        assertThat(tx.getMethod()).isEqualTo("CARD");
        assertThat(tx.getStatus()).isEqualTo("PENDING");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_doesNotOverrideExistingTimestamp() {
        PaymentTransaction tx = new PaymentTransaction();
        LocalDateTime now = LocalDateTime.now().minusDays(1);
        tx.setCreatedAt(now);

        tx.onCreate();

        assertThat(tx.getCreatedAt()).isEqualTo(now);
    }
}

