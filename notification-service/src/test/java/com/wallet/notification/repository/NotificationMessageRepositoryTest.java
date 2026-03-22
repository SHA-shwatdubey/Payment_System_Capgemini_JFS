package com.wallet.notification.repository;

import com.wallet.notification.entity.NotificationChannel;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class NotificationMessageRepositoryTest {

    @Autowired
    private NotificationMessageRepository notificationMessageRepository;

    @Test
    void customQueries_returnExpectedResults() {
        NotificationMessage sent = new NotificationMessage();
        sent.setUserId(1L);
        sent.setEventType("TOPUP");
        sent.setChannel(NotificationChannel.SMS);
        sent.setTarget("1234567890");
        sent.setMessage("credited");
        sent.setStatus(NotificationStatus.SENT);
        notificationMessageRepository.save(sent);

        NotificationMessage failed = new NotificationMessage();
        failed.setUserId(1L);
        failed.setEventType("TOPUP");
        failed.setChannel(NotificationChannel.EMAIL);
        failed.setTarget("a@b.com");
        failed.setMessage("failed");
        failed.setStatus(NotificationStatus.FAILED);
        notificationMessageRepository.save(failed);

        assertThat(notificationMessageRepository.findByUserIdOrderByCreatedAtDesc(1L)).hasSize(2);
        assertThat(notificationMessageRepository.countByStatus(NotificationStatus.SENT)).isEqualTo(1L);
        assertThat(notificationMessageRepository.countByStatus(NotificationStatus.FAILED)).isEqualTo(1L);
    }
}

