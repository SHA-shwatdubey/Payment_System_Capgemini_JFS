package com.wallet.transaction.command.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        @NotNull TransactionCommandType type,
        Long userId,
        Long senderId,
        Long receiverId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        Long originalTransactionId,
        @NotBlank String idempotencyKey
) {
}

