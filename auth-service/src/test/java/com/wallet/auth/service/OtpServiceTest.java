package com.wallet.auth.service;

import com.wallet.auth.dto.OtpRequestDto;
import com.wallet.auth.dto.OtpVerificationDto;
import com.wallet.auth.entity.OtpEntity;
import com.wallet.auth.repository.OtpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpService otpService;

    @Test
    void generateAndSendOtp_email_savesRequestAndReturnsMaskedEmail() {
        OtpRequestDto request = new OtpRequestDto("john.doe@example.com", null, "EMAIL");
        when(otpRepository.save(any(OtpEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = otpService.generateAndSendOtp(request);

        ArgumentCaptor<OtpEntity> captor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRepository).save(captor.capture());
        OtpEntity saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(saved.getOtpType()).isEqualTo("EMAIL");
        assertThat(saved.getOtp()).hasSize(6);
        assertThat(saved.getAttemptCount()).isEqualTo(0);
        assertThat(response).contains("j******e@example.com");
    }

    @Test
    void generateAndSendOtp_smsWithoutPhone_throwsException() {
        OtpRequestDto request = new OtpRequestDto(null, " ", "SMS");

        assertThatThrownBy(() -> otpService.generateAndSendOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number is required for SMS OTP");

        verify(otpRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void generateAndSendOtp_withInvalidType_throwsException() {
        OtpRequestDto request = new OtpRequestDto("john@example.com", null, "PUSH");

        assertThatThrownBy(() -> otpService.generateAndSendOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP type");
    }

    @Test
    void verifyOtp_withEmail_marksAsVerifiedAndIncrementsAttempt() {
        OtpEntity entity = new OtpEntity();
        entity.setOtp("123456");
        entity.setEmail("john@example.com");
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        entity.setAttemptCount(0);
        entity.setIsVerified(false);
        entity.setIsUsed(false);

        when(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("john@example.com", "123456"))
                .thenReturn(Optional.of(entity));

        boolean verified = otpService.verifyOtp(new OtpVerificationDto("john@example.com", null, "123456"));

        assertThat(verified).isTrue();
        assertThat(entity.getAttemptCount()).isEqualTo(1);
        assertThat(entity.getIsVerified()).isTrue();
        assertThat(entity.getIsUsed()).isTrue();
        verify(otpRepository).save(entity);
    }

    @Test
    void verifyOtp_whenExpired_throwsException() {
        OtpEntity entity = new OtpEntity();
        entity.setExpiresAt(LocalDateTime.now().minusSeconds(5));
        entity.setAttemptCount(0);

        when(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("john@example.com", "123456"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> otpService.verifyOtp(new OtpVerificationDto("john@example.com", null, "123456")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OTP has expired");
    }

    @Test
    void verifyOtp_whenAttemptsExceeded_throwsException() {
        OtpEntity entity = new OtpEntity();
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        entity.setAttemptCount(5);

        when(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("john@example.com", "123456"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> otpService.verifyOtp(new OtpVerificationDto("john@example.com", null, "123456")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Maximum OTP verification attempts exceeded");
    }

    @Test
    void verifyOtp_whenNoMatchingOtp_throwsException() {
        when(otpRepository.findByEmailAndOtpAndIsVerifiedFalse("john@example.com", "123456"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp(new OtpVerificationDto("john@example.com", null, "123456")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void cleanupExpiredOtps_deletesByCurrentTime() {
        otpService.cleanupExpiredOtps();

        verify(otpRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}

