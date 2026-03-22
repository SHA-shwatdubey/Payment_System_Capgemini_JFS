package com.wallet.auth.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.wallet.auth.dto.OtpRequestDto;
import com.wallet.auth.dto.OtpVerificationDto;
import com.wallet.auth.entity.OtpEntity;
import com.wallet.auth.repository.OtpRepository;
import java.time.LocalDateTime;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.from-number:}")
    private String twilioFromNumber;

    private boolean twilioInitialized = false;

    public String generateAndSendOtp(OtpRequestDto request) {
        String otpType = request.getOtpType() == null ? "" : request.getOtpType().trim();
        String otp = generateRandomOtp();

        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setOtp(otp);
        otpEntity.setOtpType(otpType.toUpperCase());
        otpEntity.setGeneratedAt(LocalDateTime.now());
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otpEntity.setIsUsed(false);
        otpEntity.setIsVerified(false);
        otpEntity.setAttemptCount(0);

        if ("EMAIL".equalsIgnoreCase(otpType)) {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email is required for EMAIL OTP");
            }
            otpEntity.setEmail(request.getEmail().trim());
            otpRepository.save(otpEntity);
            sendOtpViaEmail(request.getEmail().trim(), otp);
            return "OTP sent to email: " + maskEmail(request.getEmail().trim());
        }

        if ("SMS".equalsIgnoreCase(otpType)) {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                throw new IllegalArgumentException("Phone number is required for SMS OTP");
            }
            otpEntity.setPhoneNumber(request.getPhoneNumber().trim());
            otpRepository.save(otpEntity);
            sendOtpViaSms(request.getPhoneNumber().trim(), otp);
            return "OTP sent to phone: " + maskPhoneNumber(request.getPhoneNumber().trim());
        }

        throw new IllegalArgumentException("Invalid OTP type. Use EMAIL or SMS");
    }

    public boolean verifyOtp(OtpVerificationDto verification) {
        OtpEntity otpEntity = null;

        if (verification.getEmail() != null && !verification.getEmail().isBlank()) {
            otpEntity = otpRepository
                    .findByEmailAndOtpAndIsVerifiedFalse(verification.getEmail().trim(), verification.getOtp())
                    .orElse(null);
        } else if (verification.getPhoneNumber() != null && !verification.getPhoneNumber().isBlank()) {
            otpEntity = otpRepository
                    .findByPhoneNumberAndOtpAndIsVerifiedFalse(verification.getPhoneNumber().trim(), verification.getOtp())
                    .orElse(null);
        }

        if (otpEntity == null) {
            throw new RuntimeException("Invalid OTP or OTP already verified");
        }

        if (LocalDateTime.now().isAfter(otpEntity.getExpiresAt())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        Integer attempts = otpEntity.getAttemptCount() == null ? 0 : otpEntity.getAttemptCount();
        if (attempts >= MAX_ATTEMPTS) {
            throw new RuntimeException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        otpEntity.setAttemptCount(attempts + 1);
        otpEntity.setIsVerified(true);
        otpEntity.setIsUsed(true);
        otpRepository.save(otpEntity);

        return true;
    }

    private void sendOtpViaEmail(String email, String otp) {
        if (mailSender == null) {
            System.out.println("[Mock Email] OTP for " + email + ": " + otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your OTP for Wallet Authentication");
            message.setText("Your One-Time Password (OTP) is: " + otp
                    + "\n\nThis OTP is valid for 5 minutes."
                    + "\nIf you did not request this, ignore this email.");
            message.setFrom("shashwatdtech@gmail.com");
            mailSender.send(message);
            System.out.println("OTP sent to email: " + email);
        } catch (Exception e) {
            System.out.println("Email send failed, falling back to console: " + e.getMessage());
            System.out.println("[Fallback Email] OTP for " + email + ": " + otp);
        }
    }

    private void sendOtpViaSms(String phoneNumber, String otp) {
        initializeTwilio();

        if (!twilioInitialized || twilioFromNumber == null || twilioFromNumber.isBlank()) {
            System.out.println("[Mock SMS] OTP for " + phoneNumber + ": " + otp);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioFromNumber),
                    "Your Wallet OTP is: " + otp + " (valid for 5 minutes)"
            ).create();
            System.out.println("SMS sent via Twilio. SID: " + message.getSid());
        } catch (Exception e) {
            System.out.println("SMS send failed, falling back to console: " + e.getMessage());
            System.out.println("[Fallback SMS] OTP for " + phoneNumber + ": " + otp);
        }
    }

    private void initializeTwilio() {
        if (twilioInitialized) {
            return;
        }

        if (twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()) {
            return;
        }

        Twilio.init(twilioAccountSid, twilioAuthToken);
        twilioInitialized = true;
    }

    private String generateRandomOtp() {
        Random random = new Random();
        int otp = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        if (parts.length != 2) {
            return "***";
        }

        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return "*".repeat(name.length()) + "@" + domain;
        }

        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1) + "@" + domain;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        return "*".repeat(phoneNumber.length() - 3) + phoneNumber.substring(phoneNumber.length() - 3);
    }

    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}

