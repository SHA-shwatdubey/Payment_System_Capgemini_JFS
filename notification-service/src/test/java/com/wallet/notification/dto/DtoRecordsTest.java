package com.wallet.notification.dto;

import com.wallet.notification.entity.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void dtoRecords_coverBasicContract() {
        DeviceTokenRequest tokenRequest = new DeviceTokenRequest(1L, "token-1");
        NotificationEvent event = new NotificationEvent(2L, "KYC", "EMAIL", "a@b.com", "approved");
        NotificationSendRequest sendRequest = new NotificationSendRequest(3L, "TOPUP", NotificationChannel.SMS, "123", "credited");
        NotificationStatsResponse stats = new NotificationStatsResponse(4L, 1L, 5L);

        assertThat(tokenRequest.userId()).isEqualTo(1L);
        assertThat(event.channel()).isEqualTo("EMAIL");
        assertThat(sendRequest.channel()).isEqualTo(NotificationChannel.SMS);
        assertThat(stats.total()).isEqualTo(5L);
    }
}

