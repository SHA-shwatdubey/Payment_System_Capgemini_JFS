package com.wallet.notification.dto;

public record NotificationEvent(Long userId, String eventType, String channel, String target, String message) {
}

