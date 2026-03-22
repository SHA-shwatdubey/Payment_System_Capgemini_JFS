package com.wallet.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequest(
        @NotNull Long senderId,
        @NotNull Long receiverId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull Long originalTransactionId,
        @NotBlank String idempotencyKey
) {
}

