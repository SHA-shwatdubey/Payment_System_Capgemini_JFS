package com.wallet.wallet.query.controller;

import com.wallet.wallet.query.dto.WalletBalanceView;
import com.wallet.wallet.query.dto.WalletHistoryItemView;
import com.wallet.wallet.query.handler.WalletQueryHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletQueryController {

    private final WalletQueryHandler walletQueryHandler;

    public WalletQueryController(WalletQueryHandler walletQueryHandler) {
        this.walletQueryHandler = walletQueryHandler;
    }

    @GetMapping("/{userId}")
    public WalletBalanceView getWallet(@PathVariable Long userId) {
        return walletQueryHandler.getWallet(userId);
    }

    @GetMapping("/{userId}/transactions")
    public List<WalletHistoryItemView> getWalletHistory(@PathVariable Long userId) {
        return walletQueryHandler.getWalletHistory(userId);
    }
}

