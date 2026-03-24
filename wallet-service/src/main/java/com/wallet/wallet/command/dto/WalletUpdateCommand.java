package com.wallet.wallet.command.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletUpdateCommand(
        @NotNull WalletCommandType type,
        Long userId,
        Long fromUserId,
        Long toUserId,
        @DecimalMin(value = "0.01") BigDecimal amount,
        String paymentMethod,
        BigDecimal dailyTopupLimit,
        BigDecimal dailyTransferLimit,
        Integer dailyTransferCountLimit,
        @NotBlank String commandId
) {
}

