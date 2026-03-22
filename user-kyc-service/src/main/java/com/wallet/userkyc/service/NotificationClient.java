package com.wallet.userkyc.service;

import com.wallet.userkyc.config.NotificationRabbitConfig;
import com.wallet.userkyc.dto.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationClient {

    private final RabbitTemplate rabbitTemplate;

    public NotificationClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendSafe(Long userId, String eventType, String channel, String target, String message) {
        try {
            NotificationEvent event = new NotificationEvent(userId, eventType, channel, target, message);
            rabbitTemplate.convertAndSend(
                    NotificationRabbitConfig.NOTIFICATION_EXCHANGE,
                    NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY,
                    event
            );
        } catch (Exception ignored) {
            // Notification failure should not fail KYC operations.
        }
    }
}



