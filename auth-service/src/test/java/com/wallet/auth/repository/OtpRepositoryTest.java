package com.wallet.auth.repository;

import com.wallet.auth.entity.OtpEntity;
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
class OtpRepositoryTest {

    @Autowired
    private OtpRepository otpRepository;

    @Test
    void findByEmailAndOtpAndIsVerifiedFalse_returnsOnlyUnverifiedOtp() {
        OtpEntity pending = new OtpEntity();
        pending.setEmail("john@example.com");
        pending.setOtp("111111");
        pending.setOtpType("EMAIL");
        pending.setGeneratedAt(LocalDateTime.now());
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        pending.setIsVerified(false);
        pending.setIsUsed(false);
        pending.setAttemptCount(0);
        otpRepository.save(pending);

        OtpEntity verified = new OtpEntity();
        verified.setEmail("john@example.com");
        verified.setOtp("111111");
        verified.setOtpType("EMAIL");
        verified.setGeneratedAt(LocalDateTime.now());
        verified.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verified.setIsVerified(true);
        verified.setIsUsed(true);
        verified.setAttemptCount(1);
        otpRepository.save(verified);

        assertThat(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("john@example.com", "111111"))
                .isPresent()
                .get()
                .extracting(OtpEntity::getIsVerified)
                .isEqualTo(false);
    }

    @Test
    void findTopByEmailOrderByGeneratedAtDesc_returnsLatestOtp() {
        OtpEntity older = new OtpEntity();
        older.setEmail("jane@example.com");
        older.setOtp("111111");
        older.setOtpType("EMAIL");
        older.setGeneratedAt(LocalDateTime.now().minusMinutes(10));
        older.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        older.setIsVerified(false);
        older.setIsUsed(false);
        older.setAttemptCount(0);
        otpRepository.save(older);

        OtpEntity latest = new OtpEntity();
        latest.setEmail("jane@example.com");
        latest.setOtp("222222");
        latest.setOtpType("EMAIL");
        latest.setGeneratedAt(LocalDateTime.now());
        latest.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        latest.setIsVerified(false);
        latest.setIsUsed(false);
        latest.setAttemptCount(0);
        otpRepository.save(latest);

        assertThat(otpRepository.findTopByEmailOrderByGeneratedAtDesc("jane@example.com"))
                .isPresent()
                .get()
                .extracting(OtpEntity::getOtp)
                .isEqualTo("222222");
    }

    @Test
    void deleteByExpiresAtBefore_removesExpiredRecords() {
        OtpEntity expired = new OtpEntity();
        expired.setEmail("old@example.com");
        expired.setOtp("333333");
        expired.setOtpType("EMAIL");
        expired.setGeneratedAt(LocalDateTime.now().minusMinutes(30));
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expired.setIsVerified(false);
        expired.setIsUsed(false);
        expired.setAttemptCount(0);
        otpRepository.save(expired);

        OtpEntity active = new OtpEntity();
        active.setEmail("new@example.com");
        active.setOtp("444444");
        active.setOtpType("EMAIL");
        active.setGeneratedAt(LocalDateTime.now());
        active.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        active.setIsVerified(false);
        active.setIsUsed(false);
        active.setAttemptCount(0);
        otpRepository.save(active);

        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        assertThat(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("old@example.com", "333333")).isEmpty();
        assertThat(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("new@example.com", "444444")).isPresent();
    }
}

