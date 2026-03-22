package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardCatalogItemRepository extends JpaRepository<RewardCatalogItem, Long> {
	List<RewardCatalogItem> findByMerchantId(Long merchantId);
}


