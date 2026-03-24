package com.wallet.transaction.query.dto;

import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.repository.TransactionReadProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionReadDto(
        Long id,
        Long userId,
        Long senderId,
        Long receiverId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        LocalDateTime createdAt
) {
    public static TransactionReadDto fromProjection(TransactionReadProjection projection) {
        return new TransactionReadDto(
                projection.getId(),
                projection.getUserId(),
                projection.getSenderId(),
                projection.getReceiverId(),
                projection.getAmount(),
                projection.getType(),
                projection.getStatus(),
                projection.getCreatedAt()
        );
    }
}

