package com.wallet.wallet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.wallet.wallet.dto.NotificationEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void sendSafe_withValidInput_publishesEvent() {
        NotificationClient notificationClient = new NotificationClient(rabbitTemplate);

        notificationClient.sendSafe(1L, "WALLET_CREDIT", "SMS", "user-1", "credited");

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void sendSafe_whenRabbitThrows_swallowsException() {
        NotificationClient notificationClient = new NotificationClient(rabbitTemplate);
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        notificationClient.sendSafe(1L, "TRANSFER_SENT", "SMS", "user-1", "sent");
    }
}


