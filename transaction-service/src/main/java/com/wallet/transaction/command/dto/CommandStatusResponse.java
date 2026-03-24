package com.wallet.transaction.command.dto;

public record CommandStatusResponse(
        String status,
        String idempotencyKey,
        String message
) {
    public static CommandStatusResponse accepted(String idempotencyKey, String message) {
        return new CommandStatusResponse("ACCEPTED", idempotencyKey, message);
    }
}

