package com.wallet.userkyc.dto;

public record NotificationEvent(Long userId, String eventType, String channel, String target, String message) {
}

