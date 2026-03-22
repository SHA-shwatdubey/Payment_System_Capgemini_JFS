package com.wallet.transaction.repository;

import com.wallet.transaction.entity.EntryType;
import com.wallet.transaction.entity.LedgerEntry;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
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
class LedgerEntryRepositoryTest {

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void calculateBalance_combinesCreditAndDebit() {
        Transaction transaction = saveTransaction("ledger-key-1");
        saveEntry(transaction, 4L, EntryType.CREDIT, new BigDecimal("120.00"), LocalDateTime.now().minusHours(2));
        saveEntry(transaction, 4L, EntryType.DEBIT, new BigDecimal("20.00"), LocalDateTime.now().minusHours(1));

        assertThat(ledgerEntryRepository.calculateBalance(4L)).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateBalanceBefore_respectsCutoffTime() {
        Transaction transaction = saveTransaction("ledger-key-2");
        LocalDateTime pivot = LocalDateTime.now().minusMinutes(30);
        saveEntry(transaction, 8L, EntryType.CREDIT, new BigDecimal("70.00"), pivot.minusMinutes(10));
        saveEntry(transaction, 8L, EntryType.DEBIT, new BigDecimal("20.00"), pivot.plusMinutes(10));

        assertThat(ledgerEntryRepository.calculateBalanceBefore(8L, pivot)).isEqualByComparingTo("70.00");
    }

    private Transaction saveTransaction(String key) {
        Transaction transaction = new Transaction();
        transaction.setUserId(1L);
        transaction.setSenderId(0L);
        transaction.setReceiverId(1L);
        transaction.setAmount(new BigDecimal("1.00"));
        transaction.setType(TransactionType.TOPUP);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(key);
        return transactionRepository.save(transaction);
    }

    private void saveEntry(Transaction transaction, Long userId, EntryType type, BigDecimal amount, LocalDateTime createdAt) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransaction(transaction);
        entry.setUserId(userId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(createdAt);
        ledgerEntryRepository.save(entry);
    }
}

