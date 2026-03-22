package com.wallet.admin.repository;

import com.wallet.admin.entity.AdminAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

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
class AdminActionRepositoryTest {

    @Autowired
    private AdminActionRepository adminActionRepository;

    @Test
    void save_persistsAdminAction() {
        AdminAction action = new AdminAction();
        action.setActionType("CREATE_CAMPAIGN");
        action.setTargetId(8L);
        action.setStatus("SUCCESS");
        action.setReason("done");
        action.setCreatedAt(LocalDateTime.now());

        AdminAction saved = adminActionRepository.save(action);

        assertThat(saved.getId()).isNotNull();
        assertThat(adminActionRepository.findById(saved.getId())).isPresent();
    }
}

