package com.wallet.wallet.event;

import java.time.LocalDateTime;
import java.util.List;

public record WalletUpdatedEvent(
        String commandId,
        List<Long> impactedUserIds,
        LocalDateTime occurredAt
) {
}

