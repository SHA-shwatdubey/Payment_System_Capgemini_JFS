package com.wallet.transaction.dto;

public record DisputeCreateRequest(Long transactionId, Long userId, String reason) {
}

