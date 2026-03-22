package com.wallet.rewards.service;

import com.wallet.rewards.dto.WalletEvent;
import com.wallet.rewards.entity.RewardsAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletEventListenerTest {

    @Mock
    private RewardsService rewardsService;

    @Test
    void handleWalletEvent_topup_addsPoints() {
        WalletEventListener listener = new WalletEventListener(rewardsService);
        RewardsAccount account = new RewardsAccount();
        account.setTier("GOLD");

        when(rewardsService.calculateEarnPoints(new BigDecimal("200"))).thenReturn(20);
        when(rewardsService.summary(1L)).thenReturn(account);

        listener.handleWalletEvent(new WalletEvent(1L, "TOPUP", new BigDecimal("200")));

        verify(rewardsService).addPoints(eq(1L), eq(30), eq("EARN_TOPUP"));
    }

    @Test
    void handleWalletEvent_transfer_doesNothing() {
        WalletEventListener listener = new WalletEventListener(rewardsService);

        listener.handleWalletEvent(new WalletEvent(2L, "TRANSFER", new BigDecimal("100")));

        verify(rewardsService, never()).addPoints(anyLong(), anyInt(), anyString());
    }
}


