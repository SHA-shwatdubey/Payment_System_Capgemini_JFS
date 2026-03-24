package com.wallet.transaction.query.handler;

import com.wallet.transaction.config.RabbitConfig;
import com.wallet.transaction.event.TransactionCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class TransactionCacheInvalidationListener {

    private final CacheManager cacheManager;

    public TransactionCacheInvalidationListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @RabbitListener(queues = RabbitConfig.TRANSACTION_EVENTS_QUEUE)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Cache cache = cacheManager.getCache("transactionHistory");
        if (cache == null || event.impactedUserIds() == null) {
            return;
        }

        for (Long userId : event.impactedUserIds()) {
            if (userId != null) {
                cache.evict(userId);
            }
        }
    }
}

