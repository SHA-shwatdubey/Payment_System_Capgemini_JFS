package com.wallet.wallet.dto;

public record NotificationEvent(Long userId, String eventType, String channel, String target, String message) {
}

