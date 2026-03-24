package com.wallet.wallet.service;

import com.wallet.wallet.config.RabbitConfig;
import com.wallet.wallet.dto.WalletEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service("walletDomainEventPublisher")
public class WalletEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public WalletEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(WalletEvent walletEvent) {
        rabbitTemplate.convertAndSend(RabbitConfig.WALLET_EVENTS_EXCHANGE, RabbitConfig.WALLET_EVENTS_KEY, walletEvent);
    }
}

