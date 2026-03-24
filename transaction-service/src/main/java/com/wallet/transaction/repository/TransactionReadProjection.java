package com.wallet.transaction.repository;

import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionReadProjection {
    Long getId();

    Long getUserId();

    Long getSenderId();

    Long getReceiverId();

    BigDecimal getAmount();

    TransactionType getType();

    TransactionStatus getStatus();

    LocalDateTime getCreatedAt();
}

