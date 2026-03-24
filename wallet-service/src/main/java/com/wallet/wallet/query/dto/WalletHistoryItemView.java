package com.wallet.wallet.query.dto;

import com.wallet.wallet.entity.LedgerEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletHistoryItemView(
        Long id,
        String type,
        BigDecimal amount,
        String reference,
        LocalDateTime createdAt
) {
    public static WalletHistoryItemView fromEntity(LedgerEntry entry) {
        return new WalletHistoryItemView(
                entry.getId(),
                entry.getType(),
                entry.getAmount(),
                entry.getReference(),
                entry.getCreatedAt()
        );
    }
}

