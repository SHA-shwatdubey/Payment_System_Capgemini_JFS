package com.wallet.rewards;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.wallet.rewards.entity.RewardCatalogItem;
import com.wallet.rewards.repository.RewardCatalogItemRepository;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Rewards Service API", version = "1.0"))
public class RewardsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RewardsServiceApplication.class, args);
    }

    @Bean
    @ConditionalOnBean(RewardCatalogItemRepository.class)
    public CommandLineRunner initDB(RewardCatalogItemRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                RewardCatalogItem item = new RewardCatalogItem();
                item.setName("Cashback 100");
                item.setPointsCost(100);
                item.setStock(1000);
                item.setRewardType("CASHBACK");
                item.setMerchantId(0L);
                item.setRedeemCount(0);
                repository.save(item);
            }
        };
    }
}

