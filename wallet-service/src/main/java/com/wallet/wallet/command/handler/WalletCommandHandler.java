package com.wallet.wallet.command.handler;

import com.wallet.wallet.command.dto.WalletCommandType;
import com.wallet.wallet.command.dto.WalletUpdateCommand;
import com.wallet.wallet.domain.WalletDomainService;
import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.event.WalletEventPublisher;
import com.wallet.wallet.event.WalletUpdatedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class WalletCommandHandler {

    private final WalletDomainService walletDomainService;
    private final WalletEventPublisher walletEventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public WalletCommandHandler(WalletDomainService walletDomainService,
                                WalletEventPublisher walletEventPublisher,
                                StringRedisTemplate stringRedisTemplate) {
        this.walletDomainService = walletDomainService;
        this.walletEventPublisher = walletEventPublisher;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    @CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)
    public void handle(WalletUpdateCommand command) {
        Boolean accepted = stringRedisTemplate.opsForValue().setIfAbsent(
                "wallet:command:" + command.commandId(),
                "1",
                Duration.ofHours(24)
        );
        if (!Boolean.TRUE.equals(accepted)) {
            return;
        }

        Set<Long> impactedUserIds = execute(command);
        walletEventPublisher.publish(new WalletUpdatedEvent(command.commandId(), impactedUserIds.stream().toList(), LocalDateTime.now()));
    }

    private Set<Long> execute(WalletUpdateCommand command) {
        Set<Long> impactedUserIds = new LinkedHashSet<>();

        if (command.type() == WalletCommandType.TOPUP) {
            if (command.userId() == null || command.amount() == null) {
                throw new IllegalArgumentException("userId and amount are required for TOPUP command");
            }
            walletDomainService.topup(new TopupRequest(command.userId(), command.amount(), command.paymentMethod()));
            impactedUserIds.add(command.userId());
            return impactedUserIds;
        }

        if (command.type() == WalletCommandType.TRANSFER) {
            if (command.fromUserId() == null || command.toUserId() == null || command.amount() == null) {
                throw new IllegalArgumentException("fromUserId, toUserId and amount are required for TRANSFER command");
            }
            walletDomainService.transfer(new TransferRequest(command.fromUserId(), command.toUserId(), command.amount()));
            impactedUserIds.add(command.fromUserId());
            impactedUserIds.add(command.toUserId());
            return impactedUserIds;
        }

        if (command.type() == WalletCommandType.LIMITS) {
            walletDomainService.updateLimits(new WalletLimitUpdateRequest(
                    command.dailyTopupLimit(),
                    command.dailyTransferLimit(),
                    command.dailyTransferCountLimit()
            ));
            if (command.userId() != null) {
                impactedUserIds.add(command.userId());
            }
            return impactedUserIds;
        }

        throw new IllegalArgumentException("Unsupported wallet command type: " + command.type());
    }
}


