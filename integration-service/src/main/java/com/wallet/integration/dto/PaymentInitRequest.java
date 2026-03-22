package com.wallet.integration.dto;

import java.math.BigDecimal;

public record PaymentInitRequest(Long userId, BigDecimal amount, String method) {
}

