package com.wallet.notification.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {

    @Test
    void gettersSettersAndPrePersist_work() {
        NotificationMessage message = new NotificationMessage();
        message.setId(1L);
        message.setUserId(7L);
        message.setEventType("KYC");
        message.setChannel(NotificationChannel.EMAIL);
        message.setTarget("x@y.com");
        message.setMessage("approved");
        message.setStatus(NotificationStatus.SENT);
        message.setFailureReason(null);

        message.onCreate();

        assertThat(message.getId()).isEqualTo(1L);
        assertThat(message.getUserId()).isEqualTo(7L);
        assertThat(message.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingCreatedAt() {
        NotificationMessage message = new NotificationMessage();
        LocalDateTime previous = LocalDateTime.now().minusHours(2);
        message.setCreatedAt(previous);

        message.onCreate();

        assertThat(message.getCreatedAt()).isEqualTo(previous);
    }
}

