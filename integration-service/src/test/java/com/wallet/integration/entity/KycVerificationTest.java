package com.wallet.integration.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KycVerificationTest {

    @Test
    void gettersSettersAndPrePersist_workAsExpected() {
        KycVerification verification = new KycVerification();
        verification.setId(2L);
        verification.setUserId(20L);
        verification.setVerificationRef("KYC-20");
        verification.setStatus("VERIFIED");
        verification.setProviderMessage("ok");

        verification.onCreate();

        assertThat(verification.getId()).isEqualTo(2L);
        assertThat(verification.getUserId()).isEqualTo(20L);
        assertThat(verification.getVerificationRef()).isEqualTo("KYC-20");
        assertThat(verification.getStatus()).isEqualTo("VERIFIED");
        assertThat(verification.getProviderMessage()).isEqualTo("ok");
        assertThat(verification.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_keepsExistingCreatedAt() {
        KycVerification verification = new KycVerification();
        LocalDateTime previous = LocalDateTime.now().minusHours(4);
        verification.setCreatedAt(previous);

        verification.onCreate();

        assertThat(verification.getCreatedAt()).isEqualTo(previous);
    }
}

