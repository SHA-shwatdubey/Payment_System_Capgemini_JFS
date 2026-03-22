package com.wallet.transaction.service;

import com.wallet.transaction.config.RabbitConfig;
import com.wallet.transaction.dto.WalletEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RewardEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RewardEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(WalletEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.WALLET_EVENTS_EXCHANGE, RabbitConfig.WALLET_EVENTS_KEY, event);
    }
}

