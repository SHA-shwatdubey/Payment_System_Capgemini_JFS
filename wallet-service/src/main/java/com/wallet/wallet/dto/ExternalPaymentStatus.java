package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record ExternalPaymentStatus(String paymentRef, Long userId, BigDecimal amount, String method, String status) {
}

