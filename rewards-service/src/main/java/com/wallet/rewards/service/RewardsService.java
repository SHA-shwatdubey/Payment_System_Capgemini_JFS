package com.wallet.rewards.service;

import com.wallet.rewards.dto.RedeemRequest;
import com.wallet.rewards.dto.MerchantItemRequest;
import com.wallet.rewards.dto.RewardRuleUpdateRequest;
import com.wallet.rewards.entity.RewardCatalogItem;
import com.wallet.rewards.entity.RewardRuleConfig;
import com.wallet.rewards.entity.RewardsAccount;
import com.wallet.rewards.entity.RewardsTransaction;
import com.wallet.rewards.repository.RewardCatalogItemRepository;
import com.wallet.rewards.repository.RewardRuleConfigRepository;
import com.wallet.rewards.repository.RewardsAccountRepository;
import com.wallet.rewards.repository.RewardsTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RewardsService {
    private final RewardsAccountRepository rewardsAccountRepository;
    private final RewardCatalogItemRepository rewardCatalogItemRepository;
    private final RewardsTransactionRepository rewardsTransactionRepository;
    private final RewardRuleConfigRepository rewardRuleConfigRepository;
    private final NotificationClient notificationClient;

    private static final Long RULE_CONFIG_ID = 1L;

    public RewardsService(RewardsAccountRepository rewardsAccountRepository,
                          RewardCatalogItemRepository rewardCatalogItemRepository,
                          RewardsTransactionRepository rewardsTransactionRepository,
                          RewardRuleConfigRepository rewardRuleConfigRepository,
                          NotificationClient notificationClient) {
        this.rewardsAccountRepository = rewardsAccountRepository;
        this.rewardCatalogItemRepository = rewardCatalogItemRepository;
        this.rewardsTransactionRepository = rewardsTransactionRepository;
        this.rewardRuleConfigRepository = rewardRuleConfigRepository;
        this.notificationClient = notificationClient;
    }

    public RewardsAccount summary(Long userId) {
        return getOrCreate(userId);
    }

    public List<RewardCatalogItem> catalog() {
        return rewardCatalogItemRepository.findAll();
    }

    @Transactional
    public String redeem(RedeemRequest request) {
        RewardsAccount account = getOrCreate(request.userId());
        RewardCatalogItem item = rewardCatalogItemRepository.findById(request.rewardId()).orElseThrow();
        if (item.getStock() <= 0) {
            throw new IllegalArgumentException("Reward out of stock");
        }
        if (account.getPoints() < item.getPointsCost()) {
            throw new IllegalArgumentException("Insufficient points");
        }

        account.setPoints(account.getPoints() - item.getPointsCost());
        account.setTier(tierFor(account.getPoints()));
        item.setStock(item.getStock() - 1);
        item.setRedeemCount((item.getRedeemCount() == null ? 0 : item.getRedeemCount()) + 1);
        rewardsAccountRepository.save(account);
        rewardCatalogItemRepository.save(item);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(request.userId());
        transaction.setPoints(-item.getPointsCost());
        transaction.setType("REDEEM");
        transaction.setReference(UUID.randomUUID().toString());
        transaction.setCreatedAt(LocalDateTime.now());
        rewardsTransactionRepository.save(transaction);

        notificationClient.sendSafe(
                request.userId(),
                "REWARD_REDEEM",
                "EMAIL",
                "user-" + request.userId(),
                "Reward redeemed successfully: " + item.getName()
        );
        notificationClient.sendSafe(
                request.userId(),
                "REWARD_REDEEM",
                "PUSH",
                "device-user-" + request.userId(),
                "Reward redeemed: " + item.getName()
        );

        return "Redemption successful";
    }

    @Transactional
    public void addPoints(Long userId, int points, String type) {
        RewardsAccount account = getOrCreate(userId);
        account.setPoints(account.getPoints() + points);
        account.setTier(tierFor(account.getPoints()));
        rewardsAccountRepository.save(account);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setReference(UUID.randomUUID().toString());
        transaction.setCreatedAt(LocalDateTime.now());
        rewardsTransactionRepository.save(transaction);
    }

    private RewardsAccount getOrCreate(Long userId) {
        return rewardsAccountRepository.findByUserId(userId).orElseGet(() -> {
            RewardsAccount account = new RewardsAccount();
            account.setUserId(userId);
            account.setPoints(0);
            account.setTier("SILVER");
            return rewardsAccountRepository.save(account);
        });
    }

    private String tierFor(int points) {
        RewardRuleConfig config = getOrCreateRuleConfig();
        if (points >= config.getPlatinumThreshold()) {
            return "PLATINUM";
        }
        if (points >= config.getGoldThreshold()) {
            return "GOLD";
        }
        return "SILVER";
    }

    public int calculateEarnPoints(java.math.BigDecimal amount) {
        RewardRuleConfig config = getOrCreateRuleConfig();
        int blocks = amount.divideToIntegralValue(new java.math.BigDecimal("100")).intValue();
        return blocks * config.getPointsPer100();
    }

    public RewardRuleConfig getRuleConfig() {
        return getOrCreateRuleConfig();
    }

    @Transactional
    public RewardRuleConfig updateRuleConfig(RewardRuleUpdateRequest request) {
        RewardRuleConfig config = getOrCreateRuleConfig();
        if (request.pointsPer100() != null && request.pointsPer100() > 0) {
            config.setPointsPer100(request.pointsPer100());
        }
        if (request.goldThreshold() != null && request.goldThreshold() > 0) {
            config.setGoldThreshold(request.goldThreshold());
        }
        if (request.platinumThreshold() != null && request.platinumThreshold() > config.getGoldThreshold()) {
            config.setPlatinumThreshold(request.platinumThreshold());
        }
        return rewardRuleConfigRepository.save(config);
    }

    @Transactional
    public RewardCatalogItem upsertMerchantItem(MerchantItemRequest request) {
        RewardCatalogItem item = new RewardCatalogItem();
        item.setMerchantId(request.merchantId());
        item.setName(request.name());
        item.setPointsCost(request.pointsCost());
        item.setStock(request.stock());
        item.setRewardType(request.rewardType());
        item.setRedeemCount(0);
        return rewardCatalogItemRepository.save(item);
    }

    @Transactional
    public RewardCatalogItem updateMerchantItem(Long itemId, MerchantItemRequest request) {
        RewardCatalogItem item = rewardCatalogItemRepository.findById(itemId).orElseThrow();
        item.setMerchantId(request.merchantId());
        item.setName(request.name());
        item.setPointsCost(request.pointsCost());
        item.setStock(request.stock());
        item.setRewardType(request.rewardType());
        if (item.getRedeemCount() == null) {
            item.setRedeemCount(0);
        }
        return rewardCatalogItemRepository.save(item);
    }

    public java.util.List<RewardCatalogItem> merchantItems(Long merchantId) {
        return rewardCatalogItemRepository.findByMerchantId(merchantId);
    }

    private RewardRuleConfig getOrCreateRuleConfig() {
        return rewardRuleConfigRepository.findById(RULE_CONFIG_ID).orElseGet(() -> {
            RewardRuleConfig config = new RewardRuleConfig();
            config.setId(RULE_CONFIG_ID);
            config.setPointsPer100(1);
            config.setGoldThreshold(1000);
            config.setPlatinumThreshold(5000);
            return rewardRuleConfigRepository.save(config);
        });
    }
}




