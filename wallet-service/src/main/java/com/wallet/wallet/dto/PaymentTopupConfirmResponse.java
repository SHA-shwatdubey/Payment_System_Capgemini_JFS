package com.wallet.wallet.dto;

import com.wallet.wallet.entity.WalletAccount;

public record PaymentTopupConfirmResponse(String paymentRef, String paymentStatus, WalletAccount walletAccount) {
}

