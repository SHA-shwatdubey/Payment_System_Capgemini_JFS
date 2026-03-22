package com.wallet.userkyc.service;

import com.wallet.userkyc.config.NotificationRabbitConfig;
import com.wallet.userkyc.dto.NotificationEvent;
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

        client.sendSafe(1L, "KYC_STATUS_UPDATE", "EMAIL", "a@b.com", "approved");

        verify(rabbitTemplate).convertAndSend(
                eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY),
                any(NotificationEvent.class)
        );
    }

    @Test
    void sendSafe_swallowsTemplateFailure() {
        NotificationClient client = new NotificationClient(rabbitTemplate);
        doThrow(new RuntimeException("rabbit down")).when(rabbitTemplate)
                .convertAndSend(eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                        eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY), any(NotificationEvent.class));

        client.sendSafe(2L, "KYC_STATUS_UPDATE", "EMAIL", "c@d.com", "rejected");

        verify(rabbitTemplate).convertAndSend(
                eq(NotificationRabbitConfig.NOTIFICATION_EXCHANGE),
                eq(NotificationRabbitConfig.NOTIFICATION_ROUTING_KEY),
                any(NotificationEvent.class)
        );
    }
}


