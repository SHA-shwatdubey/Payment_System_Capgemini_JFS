package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record WalletLimitUpdateRequest(
        BigDecimal dailyTopupLimit,
        BigDecimal dailyTransferLimit,
        Integer dailyTransferCountLimit
) {
}

