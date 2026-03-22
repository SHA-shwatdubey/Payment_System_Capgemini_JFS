package com.wallet.wallet.repository;

import com.wallet.wallet.entity.LedgerEntry;
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

    @Test
    void sumByUserAndTypeAndCreatedAtBetween_returnsAggregatedAmount() {
        saveEntry(10L, "TOPUP", new BigDecimal("40.00"), LocalDateTime.now().minusHours(2));
        saveEntry(10L, "TOPUP", new BigDecimal("60.00"), LocalDateTime.now().minusHours(1));

        BigDecimal sum = ledgerEntryRepository.sumByUserAndTypeAndCreatedAtBetween(
                10L, "TOPUP", LocalDateTime.now().minusDays(1), LocalDateTime.now()
        );

        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    void countByUserIdAndTypeAndCreatedAtBetween_countsMatchingRows() {
        saveEntry(15L, "TRANSFER_DEBIT", new BigDecimal("-10.00"), LocalDateTime.now().minusHours(2));
        saveEntry(15L, "TRANSFER_DEBIT", new BigDecimal("-5.00"), LocalDateTime.now().minusHours(1));

        long count = ledgerEntryRepository.countByUserIdAndTypeAndCreatedAtBetween(
                15L, "TRANSFER_DEBIT", LocalDateTime.now().minusDays(1), LocalDateTime.now()
        );

        assertThat(count).isEqualTo(2L);
    }

    private void saveEntry(Long userId, String type, BigDecimal amount, LocalDateTime createdAt) {
        LedgerEntry entry = new LedgerEntry();
        entry.setUserId(userId);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setReference("ref-" + userId + "-" + createdAt.getHour());
        entry.setCreatedAt(createdAt);
        ledgerEntryRepository.save(entry);
    }
}

