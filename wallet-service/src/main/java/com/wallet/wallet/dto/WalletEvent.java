package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record WalletEvent(Long userId, String eventType, BigDecimal amount) {
}

