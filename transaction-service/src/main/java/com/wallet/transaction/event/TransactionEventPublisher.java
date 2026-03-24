package com.wallet.transaction.event;

import com.wallet.transaction.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(TransactionCreatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.CQRS_EXCHANGE, RabbitConfig.TRANSACTION_EVENTS_KEY, event);
    }
}

