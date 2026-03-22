package com.wallet.notification.service;

import com.wallet.notification.dto.DeviceTokenRequest;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.entity.DeviceToken;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.entity.NotificationStatus;
import com.wallet.notification.repository.DeviceTokenRepository;
import com.wallet.notification.repository.NotificationMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationMessageRepository messageRepository;
    private final DeviceTokenRepository tokenRepository;

    public NotificationService(NotificationMessageRepository messageRepository, DeviceTokenRepository tokenRepository) {
        this.messageRepository = messageRepository;
        this.tokenRepository = tokenRepository;
    }

    public NotificationMessage send(NotificationSendRequest request) {
        NotificationMessage message = new NotificationMessage();
        message.setUserId(request.userId());
        message.setEventType(request.eventType());
        message.setChannel(request.channel());
        message.setTarget(request.target());
        message.setMessage(request.message());

        try {
            // Integration points for Twilio/SMTP/Firebase can be plugged in here.
            message.setStatus(NotificationStatus.SENT);
            message.setFailureReason(null);
        } catch (Exception ex) {
            message.setStatus(NotificationStatus.FAILED);
            message.setFailureReason(ex.getMessage());
        }

        return messageRepository.save(message);
    }

    public List<NotificationMessage> history(Long userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public DeviceToken registerDevice(DeviceTokenRequest request) {
        DeviceToken token = tokenRepository.findByUserId(request.userId()).orElseGet(DeviceToken::new);
        token.setUserId(request.userId());
        token.setToken(request.token());
        return tokenRepository.save(token);
    }

    public NotificationStatsResponse stats() {
        long sent = messageRepository.countByStatus(NotificationStatus.SENT);
        long failed = messageRepository.countByStatus(NotificationStatus.FAILED);
        return new NotificationStatsResponse(sent, failed, sent + failed);
    }
}

