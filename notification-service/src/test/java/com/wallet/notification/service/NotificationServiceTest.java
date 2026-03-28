package com.wallet.notification.service;

import com.wallet.notification.dto.DeviceTokenRequest;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.entity.DeviceToken;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationChannel;
import com.wallet.notification.entity.NotificationStatus;
import com.wallet.notification.repository.DeviceTokenRepository;
import com.wallet.notification.repository.NotificationMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationMessageRepository messageRepository;
    @Mock private DeviceTokenRepository tokenRepository;
    @InjectMocks private NotificationService notificationService;

    @Test void send_validRequest_savesWithSentStatus() {
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        NotificationMessage result = notificationService.send(
                new NotificationSendRequest(1L, "PAYMENT", NotificationChannel.EMAIL, "user@test.com", "Payment done"));
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getFailureReason()).isNull();
        verify(messageRepository).save(any());
    }

    @Test void history_returnsByUser() {
        NotificationMessage msg = new NotificationMessage();
        when(messageRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(msg));
        List<NotificationMessage> result = notificationService.history(1L);
        assertThat(result).hasSize(1);
    }

    @Test void registerDevice_newDevice_creates() {
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        DeviceToken result = notificationService.registerDevice(new DeviceTokenRequest(1L, "fcm-token-abc"));
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getToken()).isEqualTo("fcm-token-abc");
    }

    @Test void registerDevice_existingDevice_updates() {
        DeviceToken existing = new DeviceToken();
        existing.setUserId(1L); existing.setToken("old-token");
        when(tokenRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        DeviceToken result = notificationService.registerDevice(new DeviceTokenRequest(1L, "new-token"));
        assertThat(result.getToken()).isEqualTo("new-token");
    }

    @Test void stats_returnsCounts() {
        when(messageRepository.countByStatus(NotificationStatus.SENT)).thenReturn(10L);
        when(messageRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);
        NotificationStatsResponse stats = notificationService.stats();
        assertThat(stats.sent()).isEqualTo(10L);
        assertThat(stats.failed()).isEqualTo(2L);
        assertThat(stats.total()).isEqualTo(12L);
    }
}
