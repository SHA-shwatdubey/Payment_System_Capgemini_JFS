package com.wallet.wallet.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAccountEntityTest {

    @Test
    void getterSetterTest() {
        WalletAccount account = new WalletAccount();
        account.setId(1L);
        account.setUserId(2L);
        account.setBalance(BigDecimal.valueOf(100.50));
        LocalDateTime now = LocalDateTime.now();
        account.setCreatedAt(now);

        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getUserId()).isEqualTo(2L);
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100.50));
        assertThat(account.getCreatedAt()).isEqualTo(now);
    }
}
