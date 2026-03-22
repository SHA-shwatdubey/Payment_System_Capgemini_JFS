package com.wallet.wallet.controller;

import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import com.wallet.wallet.dto.PaymentTopupConfirmResponse;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.entity.LedgerEntry;
import com.wallet.wallet.entity.WalletAccount;
import com.wallet.wallet.entity.WalletLimitConfig;
import com.wallet.wallet.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public Map<String, BigDecimal> getBalance(@RequestParam("userId") Long userId) {
        return Map.of("balance", walletService.getBalance(userId));
    }

    @PostMapping("/topup")
    public WalletAccount topup(@RequestBody TopupRequest request) {
        return walletService.topup(request);
    }

    @PostMapping("/topup/init")
    public PaymentTopupInitResponse initTopup(@RequestBody PaymentTopupInitRequest request) {
        return walletService.initTopupPayment(request);
    }

    @PostMapping("/topup/confirm/{paymentRef}")
    public PaymentTopupConfirmResponse confirmTopup(@PathVariable("paymentRef") String paymentRef) {
        return walletService.confirmTopupPayment(paymentRef);
    }

    @PostMapping("/transfer")
    public Map<String, String> transfer(@RequestBody TransferRequest request) {
        return Map.of("message", walletService.transfer(request));
    }

    @GetMapping("/transactions")
    public List<LedgerEntry> transactions(@RequestParam("userId") Long userId) {
        return walletService.history(userId);
    }

    @GetMapping("/admin/limits")
    public WalletLimitConfig limits() {
        return walletService.getLimits();
    }

    @PostMapping("/admin/limits")
    public WalletLimitConfig updateLimits(@RequestBody WalletLimitUpdateRequest request) {
        return walletService.updateLimits(request);
    }
}





