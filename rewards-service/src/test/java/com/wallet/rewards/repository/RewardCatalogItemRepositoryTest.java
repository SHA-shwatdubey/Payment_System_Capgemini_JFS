package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardCatalogItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class RewardCatalogItemRepositoryTest {

    @Autowired
    private RewardCatalogItemRepository rewardCatalogItemRepository;

    @Test
    void findByMerchantId_returnsItems() {
        RewardCatalogItem item = new RewardCatalogItem();
        item.setName("Coupon");
        item.setPointsCost(100);
        item.setStock(10);
        item.setRewardType("DISCOUNT");
        item.setMerchantId(41L);
        item.setRedeemCount(0);
        rewardCatalogItemRepository.save(item);

        assertThat(rewardCatalogItemRepository.findByMerchantId(41L)).hasSize(1);
    }
}




