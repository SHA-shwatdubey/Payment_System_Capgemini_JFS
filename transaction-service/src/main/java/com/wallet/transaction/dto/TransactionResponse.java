package com.wallet.transaction.dto;

import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long userId,
        Long senderId,
        Long receiverId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String idempotencyKey,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getSenderId(),
                transaction.getReceiverId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        );
    }
}

