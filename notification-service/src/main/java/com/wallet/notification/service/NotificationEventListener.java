package com.wallet.notification.service;

import com.wallet.notification.config.RabbitConfig;
import com.wallet.notification.dto.NotificationEvent;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.entity.NotificationChannel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onNotificationEvent(NotificationEvent event) {
        if (event == null || event.userId() == null || event.channel() == null) {
            return;
        }

        try {
            NotificationSendRequest request = new NotificationSendRequest(
                    event.userId(),
                    event.eventType(),
                    NotificationChannel.valueOf(event.channel().toUpperCase()),
                    event.target(),
                    event.message()
            );
            notificationService.send(request);
        } catch (Exception ignored) {
            // Preserve previous best-effort behavior for non-blocking notifications.
        }
    }
}

