package com.wallet.notification.dto;

import com.wallet.notification.entity.NotificationChannel;

public record NotificationSendRequest(
        Long userId,
        String eventType,
        NotificationChannel channel,
        String target,
        String message
) {
}

