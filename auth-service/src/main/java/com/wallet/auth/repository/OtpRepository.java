package com.wallet.auth.repository;

import com.wallet.auth.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmailAndOtpAndIsVerifiedFalse(String email, String otp);
    Optional<OtpEntity> findByPhoneNumberAndOtpAndIsVerifiedFalse(String phoneNumber, String otp);
    Optional<OtpEntity> findTopByEmailOrderByGeneratedAtDesc(String email);
    Optional<OtpEntity> findTopByPhoneNumberOrderByGeneratedAtDesc(String phoneNumber);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}

