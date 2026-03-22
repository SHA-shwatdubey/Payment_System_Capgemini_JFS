package com.wallet.rewards.service;

import com.wallet.rewards.dto.WalletEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletEventListener {
    private final RewardsService rewardsService;

    public WalletEventListener(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }

    @RabbitListener(queues = "wallet.events.queue")
    public void handleWalletEvent(WalletEvent walletEvent) {
        if ("TRANSFER".equals(walletEvent.eventType()) || "REFUND".equals(walletEvent.eventType())) {
            return;
        }

        int basePoints = rewardsService.calculateEarnPoints(walletEvent.amount());
        if (basePoints <= 0) {
            return;
        }

        String tier = rewardsService.summary(walletEvent.userId()).getTier();
        BigDecimal multiplier = switch (tier == null ? "SILVER" : tier.toUpperCase()) {
            case "GOLD" -> new BigDecimal("1.5");
            case "PLATINUM" -> new BigDecimal("2.0");
            default -> BigDecimal.ONE;
        };

        int finalPoints = multiplier.multiply(new BigDecimal(basePoints)).intValue();
        if ("TOPUP".equals(walletEvent.eventType()) && finalPoints > 0) {
            rewardsService.addPoints(walletEvent.userId(), finalPoints, "EARN_TOPUP");
        }
        if ("PAYMENT".equals(walletEvent.eventType()) && finalPoints > 0) {
            rewardsService.addPoints(walletEvent.userId(), finalPoints, "EARN_PAYMENT");
        }
    }
}


