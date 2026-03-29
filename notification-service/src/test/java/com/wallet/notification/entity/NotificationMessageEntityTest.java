package com.wallet.notification.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageEntityTest {

    @Test
    void getterSetterTest() {
        NotificationMessage m = new NotificationMessage();
        m.setId(1L);
        m.setUserId(2L);
        m.setEventType("TEST");
        m.setChannel(NotificationChannel.SMS);
        m.setTarget("12345");
        m.setMessage("hello");
        m.setStatus(NotificationStatus.SENT);
        m.setFailureReason("none");
        LocalDateTime now = LocalDateTime.now();
        m.setCreatedAt(now);

        assertThat(m.getId()).isEqualTo(1L);
        assertThat(m.getUserId()).isEqualTo(2L);
        assertThat(m.getEventType()).isEqualTo("TEST");
        assertThat(m.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(m.getTarget()).isEqualTo("12345");
        assertThat(m.getMessage()).isEqualTo("hello");
        assertThat(m.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(m.getFailureReason()).isEqualTo("none");
        assertThat(m.getCreatedAt()).isEqualTo(now);
    }
}
