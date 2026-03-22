package com.wallet.rewards.service;

import com.wallet.rewards.config.NotificationRabbitConfig;
import com.wallet.rewards.dto.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void sendSafe_publishesEvent() {
        NotificationClient client = new NotificationClient(rabbitTemplate);

        client.sendSafe(1L, "POINTS_EARNED", "EMAIL", "a@b.com", "earned");

        verify(rabbitTemplate).convertAndSend(
                eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY),
                any(NotificationEvent.class)
        );
    }

    @Test
    void sendSafe_swallowsRabbitErrors() {
        NotificationClient client = new NotificationClient(rabbitTemplate);
        doThrow(new RuntimeException("rabbit down")).when(rabbitTemplate)
                .convertAndSend(eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                        eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY), any(NotificationEvent.class));

        client.sendSafe(2L, "POINTS_REDEEMED", "EMAIL", "x@y.com", "redeemed");

        verify(rabbitTemplate).convertAndSend(
                eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY),
                any(NotificationEvent.class)
        );
    }
}

