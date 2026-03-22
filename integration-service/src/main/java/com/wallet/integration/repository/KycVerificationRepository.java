package com.wallet.integration.repository;

import com.wallet.integration.entity.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {
    Optional<KycVerification> findByVerificationRef(String verificationRef);
}

