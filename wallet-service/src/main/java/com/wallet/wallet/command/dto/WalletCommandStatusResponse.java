package com.wallet.wallet.command.dto;

public record WalletCommandStatusResponse(
        String status,
        String commandId,
        String message
) {
    public static WalletCommandStatusResponse accepted(String commandId, String message) {
        return new WalletCommandStatusResponse("ACCEPTED", commandId, message);
    }
}

