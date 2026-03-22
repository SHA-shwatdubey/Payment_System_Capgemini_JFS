package com.wallet.rewards.controller;

import com.wallet.rewards.dto.RedeemRequest;
import com.wallet.rewards.dto.MerchantItemRequest;
import com.wallet.rewards.dto.RewardRuleUpdateRequest;
import com.wallet.rewards.entity.RewardCatalogItem;
import com.wallet.rewards.entity.RewardRuleConfig;
import com.wallet.rewards.entity.RewardsAccount;
import com.wallet.rewards.service.RewardsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
public class RewardsController {
    private final RewardsService rewardsService;

    public RewardsController(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }

    @GetMapping("/summary")
    public RewardsAccount summary(@RequestParam("userId") Long userId) {
        return rewardsService.summary(userId);
    }

    @GetMapping("/catalog")
    public List<RewardCatalogItem> catalog() {
        return rewardsService.catalog();
    }

    @PostMapping("/redeem")
    public Map<String, String> redeem(@RequestBody RedeemRequest request) {
        return Map.of("message", rewardsService.redeem(request));
    }

    @GetMapping("/admin/rules")
    public RewardRuleConfig rules() {
        return rewardsService.getRuleConfig();
    }

    @PostMapping("/admin/rules")
    public RewardRuleConfig updateRules(@RequestBody RewardRuleUpdateRequest request) {
        return rewardsService.updateRuleConfig(request);
    }

    @PostMapping("/merchant/items")
    public RewardCatalogItem createMerchantItem(@RequestBody MerchantItemRequest request) {
        return rewardsService.upsertMerchantItem(request);
    }

    @PostMapping("/merchant/items/{itemId}")
    public RewardCatalogItem updateMerchantItem(@PathVariable("itemId") Long itemId,
                                                @RequestBody MerchantItemRequest request) {
        return rewardsService.updateMerchantItem(itemId, request);
    }

    @GetMapping("/merchant/items")
    public List<RewardCatalogItem> merchantItems(@RequestParam("merchantId") Long merchantId) {
        return rewardsService.merchantItems(merchantId);
    }
}



