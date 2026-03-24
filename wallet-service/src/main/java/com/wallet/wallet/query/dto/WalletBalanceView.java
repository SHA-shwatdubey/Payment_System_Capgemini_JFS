package com.wallet.wallet.query.dto;

import java.math.BigDecimal;

public record WalletBalanceView(
        Long userId,
        BigDecimal balance
) {
}

