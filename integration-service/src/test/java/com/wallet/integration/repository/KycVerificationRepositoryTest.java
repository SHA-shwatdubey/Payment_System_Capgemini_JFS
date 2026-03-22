package com.wallet.integration.repository;

import com.wallet.integration.entity.KycVerification;
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
class KycVerificationRepositoryTest {

    @Autowired
    private KycVerificationRepository kycVerificationRepository;

    @Test
    void findByVerificationRef_returnsSavedEntity() {
        KycVerification verification = new KycVerification();
        verification.setUserId(1L);
        verification.setVerificationRef("KYC-REF-1");
        verification.setStatus("VERIFIED");
        verification.setProviderMessage("done");
        kycVerificationRepository.save(verification);

        assertThat(kycVerificationRepository.findByVerificationRef("KYC-REF-1"))
                .isPresent()
                .get()
                .extracting(KycVerification::getStatus)
                .isEqualTo("VERIFIED");
    }
}

