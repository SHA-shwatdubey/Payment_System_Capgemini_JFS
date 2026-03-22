package com.wallet.transaction.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntitiesTest {

    @Test
    void transactionAndLedgerEntry_gettersAndSetters_work() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUserId(2L);
        transaction.setSenderId(3L);
        transaction.setReceiverId(4L);
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey("k-1");

        LedgerEntry entry = new LedgerEntry();
        entry.setId(7L);
        entry.setTransaction(transaction);
        entry.setUserId(3L);
        entry.setEntryType(EntryType.DEBIT);
        entry.setAmount(new BigDecimal("15.00"));

        Dispute dispute = new Dispute();
        dispute.setId(9L);
        dispute.setTransactionId(1L);
        dispute.setUserId(2L);
        dispute.setStatus("OPEN");
        dispute.setReason("desc");
        dispute.setEscalated(false);

        assertThat(transaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(dispute.getStatus()).isEqualTo("OPEN");
    }
}

