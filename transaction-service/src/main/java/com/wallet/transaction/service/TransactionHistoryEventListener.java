package com.wallet.transaction.service;

import com.wallet.transaction.config.RabbitConfig;
import com.wallet.transaction.dto.TransactionHistoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class TransactionHistoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryEventListener.class);

    @RabbitListener(queues = RabbitConfig.TRANSACTION_EVENTS_QUEUE)
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public void handleTransactionHistoryEvent(TransactionHistoryEvent event) {
        try {
            if (event == null || event.getTransactionId() == null) {
                log.warn("Received invalid transaction history event: {}", event);
                return;
            }

            log.info("Processing transaction history event: transactionId={}, userId={}, type={}, status={}, amount={}", 
                    event.getTransactionId(), event.getUserId(), event.getType(), event.getStatus(), event.getAmount());

            // This listener primarily serves to invalidate the cache for transactionHistory
            // The @CacheEvict annotation ensures that whenever a new transaction is published,
            // any cached transaction history is cleared, forcing a fresh database query on next request

            log.debug("Transaction history cache invalidated for transactionId={}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transaction history event: {}", event, e);
            // Don't re-throw to prevent message being rejected and requeued infinitely
        }
    }
}

