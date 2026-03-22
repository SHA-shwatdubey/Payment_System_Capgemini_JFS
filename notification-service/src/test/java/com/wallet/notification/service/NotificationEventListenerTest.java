package com.wallet.notification.service;

import com.wallet.notification.dto.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void onNotificationEvent_withValidEvent_delegatesToService() {
        NotificationEventListener listener = new NotificationEventListener(notificationService);

        listener.onNotificationEvent(new NotificationEvent(1L, "KYC", "email", "a@b.com", "approved"));

        verify(notificationService).send(any());
    }

    @Test
    void onNotificationEvent_withInvalidEvent_ignoresMessage() {
        NotificationEventListener listener = new NotificationEventListener(notificationService);

        listener.onNotificationEvent(new NotificationEvent(null, "KYC", "EMAIL", "x", "m"));

        verify(notificationService, never()).send(any());
    }

    @Test
    void onNotificationEvent_whenServiceThrows_swallowsException() {
        NotificationEventListener listener = new NotificationEventListener(notificationService);
        when(notificationService.send(any())).thenThrow(new RuntimeException("mail down"));

        listener.onNotificationEvent(new NotificationEvent(2L, "TOPUP", "SMS", "1234567890", "done"));

        verify(notificationService).send(any());
    }
}

