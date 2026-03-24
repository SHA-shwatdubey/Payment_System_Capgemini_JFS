package com.wallet.wallet.event;

import com.wallet.wallet.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WalletEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public WalletEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(WalletUpdatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.CQRS_EXCHANGE, RabbitConfig.WALLET_CQRS_EVENTS_KEY, event);
    }
}

