package com.wallet.notification.service;

import com.wallet.notification.dto.DeviceTokenRequest;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.entity.DeviceToken;
import com.wallet.notification.entity.NotificationChannel;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationStatus;
import com.wallet.notification.repository.DeviceTokenRepository;
import com.wallet.notification.repository.NotificationMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMessageRepository messageRepository;

    @Mock
    private DeviceTokenRepository tokenRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void send_savesMessageAsSent() {
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationMessage result = notificationService.send(new NotificationSendRequest(1L, "TEST", NotificationChannel.SMS, "12345", "Hello"));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(messageRepository).save(any());
    }

    @Test
    void history_returnsList() {
        when(messageRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        assertThat(notificationService.history(1L)).isEmpty();
    }

    @Test
    void registerDevice_setsTokenForUser() {
        DeviceToken token = new DeviceToken();
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeviceToken result = notificationService.registerDevice(new DeviceTokenRequest(1L, "NEW-TOKEN"));

        assertThat(result.getToken()).isEqualTo("NEW-TOKEN");
    }

    @Test
    void stats_returnsAggregateCounts() {
        when(messageRepository.countByStatus(NotificationStatus.SENT)).thenReturn(10L);
        when(messageRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);

        NotificationStatsResponse stats = notificationService.stats();

        assertThat(stats.sent()).isEqualTo(10L);
        assertThat(stats.failed()).isEqualTo(2L);
        assertThat(stats.total()).isEqualTo(12L);
    }
}
