package com.wallet.transaction.event;

import java.time.LocalDateTime;
import java.util.List;

public record TransactionCreatedEvent(
        String idempotencyKey,
        List<Long> impactedUserIds,
        LocalDateTime occurredAt
) {
}

