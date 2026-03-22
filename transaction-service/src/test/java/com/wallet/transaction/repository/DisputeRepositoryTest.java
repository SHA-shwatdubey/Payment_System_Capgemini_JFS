package com.wallet.transaction.repository;

import com.wallet.transaction.entity.Dispute;
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
class DisputeRepositoryTest {

    @Autowired
    private DisputeRepository disputeRepository;

    @Test
    void findByStatusOrderByCreatedAtDesc_returnsNewestFirst() {
        Dispute older = saveDispute(4L, "OPEN", LocalDateTime.now().minusHours(2));
        Dispute newer = saveDispute(4L, "OPEN", LocalDateTime.now().minusHours(1));

        assertThat(disputeRepository.findByStatusOrderByCreatedAtDesc("OPEN"))
                .extracting(Dispute::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_filtersByUser() {
        saveDispute(20L, "OPEN", LocalDateTime.now().minusHours(2));
        saveDispute(21L, "OPEN", LocalDateTime.now().minusHours(1));

        assertThat(disputeRepository.findByUserIdOrderByCreatedAtDesc(20L)).hasSize(1);
    }

    private Dispute saveDispute(Long userId, String status, LocalDateTime createdAt) {
        Dispute dispute = new Dispute();
        dispute.setTransactionId(100L);
        dispute.setUserId(userId);
        dispute.setReason("test");
        dispute.setStatus(status);
        dispute.setEscalated(false);
        dispute.setCreatedAt(createdAt);
        dispute.setUpdatedAt(createdAt);
        return disputeRepository.save(dispute);
    }
}

