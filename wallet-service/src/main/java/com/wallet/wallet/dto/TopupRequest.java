package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record TopupRequest(Long userId, BigDecimal amount, String paymentMethod) {
}

