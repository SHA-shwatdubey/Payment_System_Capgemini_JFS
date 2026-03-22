package com.wallet.admin.repository;

import com.wallet.admin.entity.Campaign;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

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
class CampaignRepositoryTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    void save_persistsCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Summer");
        campaign.setRuleType("TOPUP");
        campaign.setBonusPoints(30);
        campaign.setStartDate(LocalDate.now());
        campaign.setEndDate(LocalDate.now().plusDays(10));

        Campaign saved = campaignRepository.save(campaign);

        assertThat(saved.getId()).isNotNull();
        assertThat(campaignRepository.findById(saved.getId())).isPresent();
    }
}






