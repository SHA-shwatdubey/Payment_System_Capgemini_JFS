package com.wallet.integration.dto;

import java.math.BigDecimal;

public record PaymentStatusResponse(String paymentRef, Long userId, BigDecimal amount, String method, String status) {
}

