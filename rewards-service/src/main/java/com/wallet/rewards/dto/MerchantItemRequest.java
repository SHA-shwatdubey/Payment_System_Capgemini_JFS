package com.wallet.rewards.dto;

public record MerchantItemRequest(Long merchantId, String name, Integer pointsCost, Integer stock, String rewardType) {
}

