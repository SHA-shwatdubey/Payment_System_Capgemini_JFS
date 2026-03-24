package com.wallet.transaction.service;

import com.wallet.transaction.config.RabbitConfig;
import com.wallet.transaction.dto.TransactionHistoryEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service("transactionHistoryEventPublisher")
public class TransactionEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTransactionEvent(TransactionHistoryEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.CQRS_EXCHANGE,
                RabbitConfig.TRANSACTION_EVENTS_KEY,
                event
        );
    }
}
