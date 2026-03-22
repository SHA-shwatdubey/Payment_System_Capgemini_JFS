package com.wallet.notification.repository;

import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    List<NotificationMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(NotificationStatus status);
}

