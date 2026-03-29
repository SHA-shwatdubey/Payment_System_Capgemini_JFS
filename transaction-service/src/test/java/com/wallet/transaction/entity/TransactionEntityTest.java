package com.wallet.transaction.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityTest {

    @Test
    void getterSetterTest() {
        Transaction t = new Transaction();
        t.setId(101L);
        t.setUserId(1L);
        t.setSenderId(11L);
        t.setReceiverId(22L);
        t.setAmount(BigDecimal.TEN);
        t.setType(TransactionType.PAYMENT);
        t.setStatus(TransactionStatus.SUCCESS);
        t.setIdempotencyKey("KEY-123");
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);

        assertThat(t.getId()).isEqualTo(101L);
        assertThat(t.getUserId()).isEqualTo(1L);
        assertThat(t.getSenderId()).isEqualTo(11L);
        assertThat(t.getReceiverId()).isEqualTo(22L);
        assertThat(t.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(t.getType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(t.getIdempotencyKey()).isEqualTo("KEY-123");
        assertThat(t.getCreatedAt()).isEqualTo(now);
    }
}
