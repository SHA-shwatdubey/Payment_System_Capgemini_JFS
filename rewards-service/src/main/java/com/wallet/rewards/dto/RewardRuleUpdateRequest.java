package com.wallet.rewards.dto;

public record RewardRuleUpdateRequest(Integer pointsPer100, Integer goldThreshold, Integer platinumThreshold) {
}

