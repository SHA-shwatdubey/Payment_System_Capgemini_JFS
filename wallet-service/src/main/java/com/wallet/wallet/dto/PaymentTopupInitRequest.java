package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record PaymentTopupInitRequest(Long userId, BigDecimal amount, String method) {
}

