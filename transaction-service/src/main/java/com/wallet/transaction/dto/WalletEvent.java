package com.wallet.transaction.dto;

import java.math.BigDecimal;

public record WalletEvent(Long userId, String eventType, BigDecimal amount) {
}

