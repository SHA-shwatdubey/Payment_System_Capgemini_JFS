package com.wallet.rewards.dto;

import java.math.BigDecimal;

public record WalletEvent(Long userId, String eventType, BigDecimal amount) {
}

