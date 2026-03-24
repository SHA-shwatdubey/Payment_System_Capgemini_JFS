package com.wallet.wallet.domain;

import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.entity.WalletAccount;
import com.wallet.wallet.entity.WalletLimitConfig;
import com.wallet.wallet.service.WalletService;
import org.springframework.stereotype.Service;

@Service
public class WalletDomainService {

    private final WalletService walletService;

    public WalletDomainService(WalletService walletService) {
        this.walletService = walletService;
    }

    public WalletAccount topup(TopupRequest request) {
        return walletService.topup(request);
    }

    public String transfer(TransferRequest request) {
        return walletService.transfer(request);
    }

    public WalletLimitConfig updateLimits(WalletLimitUpdateRequest request) {
        return walletService.updateLimits(request);
    }
}

