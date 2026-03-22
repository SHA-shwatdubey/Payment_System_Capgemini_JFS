package com.wallet.wallet.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WalletEntitiesTest {

    @Test
    void entities_gettersAndSetters_work() {
        WalletAccount walletAccount = new WalletAccount();
        walletAccount.setId(1L);
        walletAccount.setUserId(2L);
        walletAccount.setBalance(new BigDecimal("100.00"));

        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setId(3L);
        ledgerEntry.setUserId(2L);
        ledgerEntry.setType("TOPUP");
        ledgerEntry.setAmount(new BigDecimal("100.00"));
        ledgerEntry.setReference("ref-1");
        ledgerEntry.setCreatedAt(LocalDateTime.now());

        WalletLimitConfig limitConfig = new WalletLimitConfig();
        limitConfig.setId(1L);
        limitConfig.setDailyTopupLimit(new BigDecimal("50000.00"));
        limitConfig.setDailyTransferLimit(new BigDecimal("25000.00"));
        limitConfig.setDailyTransferCountLimit(10);

        assertThat(walletAccount.getBalance()).isEqualByComparingTo("100.00");
        assertThat(ledgerEntry.getType()).isEqualTo("TOPUP");
        assertThat(limitConfig.getDailyTransferCountLimit()).isEqualTo(10);
    }
}

