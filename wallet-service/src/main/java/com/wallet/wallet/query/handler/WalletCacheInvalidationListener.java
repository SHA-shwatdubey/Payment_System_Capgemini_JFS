package com.wallet.wallet.query.handler;

import com.wallet.wallet.config.RabbitConfig;
import com.wallet.wallet.event.WalletUpdatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class WalletCacheInvalidationListener {

    private final CacheManager cacheManager;

    public WalletCacheInvalidationListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @RabbitListener(queues = RabbitConfig.WALLET_CQRS_EVENTS_QUEUE)
    public void onWalletUpdated(WalletUpdatedEvent event) {
        Cache balanceCache = cacheManager.getCache("walletBalance");
        Cache historyCache = cacheManager.getCache("walletHistory");

        if (event.impactedUserIds() == null) {
            return;
        }

        for (Long userId : event.impactedUserIds()) {
            if (userId == null) {
                continue;
            }
            if (balanceCache != null) {
                balanceCache.evict(userId);
            }
            if (historyCache != null) {
                historyCache.evict(userId);
            }
        }
    }
}

