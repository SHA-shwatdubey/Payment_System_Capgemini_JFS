package com.wallet.rewards.service;

import com.wallet.rewards.dto.RedeemRequest;
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

    @Test
    void calculateEarnPoints_returnsConfiguredPoints() {
        RewardRuleConfig config = new RewardRuleConfig();
        config.setId(1L);
        config.setPointsPer100(2);
        config.setGoldThreshold(1000);
        config.setPlatinumThreshold(5000);
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        int result = rewardsService.calculateEarnPoints(new BigDecimal("350"));

        assertThat(result).isEqualTo(6);
    }

    @Test
    void redeem_whenOutOfStock_throwsException() {
        RewardsAccount account = new RewardsAccount();
        account.setUserId(1L);
        account.setPoints(500);
        account.setTier("SILVER");

        RewardCatalogItem item = new RewardCatalogItem();
        item.setId(9L);
        item.setPointsCost(100);
        item.setStock(0);

        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(rewardCatalogItemRepository.findById(9L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> rewardsService.redeem(new RedeemRequest(1L, 9L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    void addPoints_updatesAccountAndSavesTransaction() {
        RewardsAccount account = new RewardsAccount();
        account.setUserId(1L);
        account.setPoints(200);
        account.setTier("SILVER");

        RewardRuleConfig config = new RewardRuleConfig();
        config.setId(1L);
        config.setPointsPer100(2);
        config.setGoldThreshold(1000);
        config.setPlatinumThreshold(5000);

        when(rewardsAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(rewardRuleConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(rewardsAccountRepository.save(any(RewardsAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rewardsService.addPoints(1L, 100, "TOPUP");

        assertThat(account.getPoints()).isEqualTo(300);
        verify(rewardsTransactionRepository).save(any());
    }
}



