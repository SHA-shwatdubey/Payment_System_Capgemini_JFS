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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void send_withValidRequest_marksSent() {
        when(messageRepository.save(any(NotificationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationMessage result = notificationService.send(
                new NotificationSendRequest(1L, "TOPUP", NotificationChannel.SMS, "user-1", "credited")
        );

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    void registerDevice_updatesExistingToken() {
        DeviceToken existing = new DeviceToken();
        existing.setId(2L);
        existing.setUserId(5L);
        existing.setToken("old");

        when(tokenRepository.findByUserId(5L)).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken result = notificationService.registerDevice(new DeviceTokenRequest(5L, "new-token"));

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getToken()).isEqualTo("new-token");
    }

    @Test
    void stats_returnsAggregatedCounts() {
        when(messageRepository.countByStatus(NotificationStatus.SENT)).thenReturn(4L);
        when(messageRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(1L);

        NotificationStatsResponse result = notificationService.stats();

        assertThat(result.sent()).isEqualTo(4L);
        assertThat(result.failed()).isEqualTo(1L);
        assertThat(result.total()).isEqualTo(5L);
    }
}

