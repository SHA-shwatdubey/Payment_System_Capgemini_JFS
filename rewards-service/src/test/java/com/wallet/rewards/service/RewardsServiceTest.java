package com.wallet.rewards.service;

import com.wallet.rewards.dto.MerchantItemRequest;
import com.wallet.rewards.dto.RedeemRequest;
import com.wallet.rewards.dto.RewardRuleUpdateRequest;
import com.wallet.rewards.entity.RewardCatalogItem;
import com.wallet.rewards.entity.RewardRuleConfig;
import com.wallet.rewards.entity.RewardsAccount;
import com.wallet.rewards.repository.RewardCatalogItemRepository;
import com.wallet.rewards.repository.RewardRuleConfigRepository;
import com.wallet.rewards.repository.RewardsAccountRepository;
import com.wallet.rewards.repository.RewardsTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

    @Mock
    private RewardsAccountRepository rewardsAccountRepository;
    @Mock
    private RewardCatalogItemRepository rewardCatalogItemRepository;
    @Mock
    private RewardsTransactionRepository rewardsTransactionRepository;
    @Mock
    private RewardRuleConfigRepository rewardRuleConfigRepository;
    @Mock
    private NotificationClient notificationClient;
    @InjectMocks
    private RewardsService rewardsService;

    private RewardRuleConfig defaultConfig() {
        RewardRuleConfig c = new RewardRuleConfig();
        c.setId(1L);
        c.setPointsPer100(2);
        c.setGoldThreshold(1000);
        c.setPlatinumThreshold(5000);
        return c;
    }

    private RewardsAccount account(Long userId, int points) {
        RewardsAccount a = new RewardsAccount();
        a.setUserId(userId);
        a.setPoints(points);
        a.setTier("SILVER");
        return a;
    }

    @Test
    void summary_existingUser_returnsAccount() {
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, 100)));
        assertThat(rewardsService.summary(1L).getPoints()).isEqualTo(100);
    }

    @Test
    void summary_newUser_createsAccount() {
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(rewardsAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        RewardsAccount result = rewardsService.summary(1L);
        assertThat(result.getPoints()).isZero();
        assertThat(result.getTier()).isEqualTo("SILVER");
    }

    @Test
    void catalog_returnsList() {
        RewardCatalogItem item = new RewardCatalogItem();
        when(rewardCatalogItemRepository.findAll()).thenReturn(List.of(item));
        assertThat(rewardsService.catalog()).hasSize(1);
    }

    @Test
    void calculateEarnPoints_returnsConfiguredPoints() {
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));
        assertThat(rewardsService.calculateEarnPoints(new BigDecimal("350"))).isEqualTo(6);
    }

    @Test
    void redeem_success_deductsPointsAndStock() {
        RewardsAccount acc = account(1L, 500);
        RewardCatalogItem item = new RewardCatalogItem();
        item.setId(9L);
        item.setPointsCost(100);
        item.setStock(5);
        item.setName("Gift");
        item.setRedeemCount(0);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(rewardCatalogItemRepository.findById(9L)).thenReturn(Optional.of(item));
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));

        String result = rewardsService.redeem(new RedeemRequest(1L, 9L));
        assertThat(result).isEqualTo("Redemption successful");
        assertThat(acc.getPoints()).isEqualTo(400);
        assertThat(item.getStock()).isEqualTo(4);
        assertThat(item.getRedeemCount()).isEqualTo(1);
        verify(rewardsTransactionRepository).save(any());
    }

    @Test
    void redeem_outOfStock_throws() {
        RewardCatalogItem item = new RewardCatalogItem();
        item.setStock(0);
        item.setPointsCost(100);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, 500)));
        when(rewardCatalogItemRepository.findById(9L)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> rewardsService.redeem(new RedeemRequest(1L, 9L)))
                .hasMessageContaining("out of stock");
    }

    @Test
    void redeem_insufficientPoints_throws() {
        RewardCatalogItem item = new RewardCatalogItem();
        item.setStock(5);
        item.setPointsCost(1000);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, 50)));
        when(rewardCatalogItemRepository.findById(9L)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> rewardsService.redeem(new RedeemRequest(1L, 9L)))
                .hasMessageContaining("Insufficient points");
    }

    @Test
    void addPoints_updatesAndSaves() {
        RewardsAccount acc = account(1L, 200);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));
        when(rewardsAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        rewardsService.addPoints(1L, 100, "TOPUP");
        assertThat(acc.getPoints()).isEqualTo(300);
        verify(rewardsTransactionRepository).save(any());
    }

    @Test
    void addPoints_reachesGoldTier() {
        RewardsAccount acc = account(1L, 900);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));
        when(rewardsAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        rewardsService.addPoints(1L, 200, "TOPUP");
        assertThat(acc.getTier()).isEqualTo("GOLD");
    }

    @Test
    void addPoints_reachesPlatinumTier() {
        RewardsAccount acc = account(1L, 4900);
        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));
        when(rewardsAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        rewardsService.addPoints(1L, 200, "TOPUP");
        assertThat(acc.getTier()).isEqualTo("PLATINUM");
    }

    @Test
    void getRuleConfig_returnsConfig() {
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(defaultConfig()));
        assertThat(rewardsService.getRuleConfig().getPointsPer100()).isEqualTo(2);
    }

    @Test
    void getRuleConfig_createsDefault() {
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(rewardRuleConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(rewardsService.getRuleConfig().getPointsPer100()).isEqualTo(1);
    }

    @Test
    void updateRuleConfig_updatesFields() {
        RewardRuleConfig config = defaultConfig();
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(rewardRuleConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        rewardsService.updateRuleConfig(new RewardRuleUpdateRequest(5, 2000, 8000));
        assertThat(config.getPointsPer100()).isEqualTo(5);
        assertThat(config.getGoldThreshold()).isEqualTo(2000);
        assertThat(config.getPlatinumThreshold()).isEqualTo(8000);
    }

    @Test
    void updateRuleConfig_ignoresInvalidValues() {
        RewardRuleConfig config = defaultConfig();
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(rewardRuleConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        rewardsService.updateRuleConfig(new RewardRuleUpdateRequest(null, null, null));
        assertThat(config.getPointsPer100()).isEqualTo(2);
    }

    @Test
    void upsertMerchantItem_creates() {
        when(rewardCatalogItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        RewardCatalogItem result = rewardsService.upsertMerchantItem(
                new MerchantItemRequest(1L, "Coffee", 50, 100, "VOUCHER"));
        assertThat(result.getName()).isEqualTo("Coffee");
        assertThat(result.getRedeemCount()).isZero();
    }

    @Test
    void updateMerchantItem_updatesExisting() {
        RewardCatalogItem existing = new RewardCatalogItem();
        existing.setRedeemCount(null);
        when(rewardCatalogItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rewardCatalogItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        RewardCatalogItem result = rewardsService.updateMerchantItem(1L,
                new MerchantItemRequest(1L, "Tea", 30, 50, "VOUCHER"));
        assertThat(result.getName()).isEqualTo("Tea");
        assertThat(result.getRedeemCount()).isZero();
    }

    @Test
    void merchantItems_returnsByMerchant() {
        when(rewardCatalogItemRepository.findByMerchantId(1L)).thenReturn(List.of(new RewardCatalogItem()));
        assertThat(rewardsService.merchantItems(1L)).hasSize(1);
    }
}
