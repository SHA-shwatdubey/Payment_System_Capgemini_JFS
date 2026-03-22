package com.wallet.auth.controller;

import com.wallet.auth.dto.OtpRequestDto;
import com.wallet.auth.dto.OtpVerificationDto;
import com.wallet.auth.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    /**
     * Generate and send OTP via Email or SMS
     * Request: {
     *   "email": "user@example.com",
     *   "phoneNumber": "9555660256",
     *   "otpType": "EMAIL" or "SMS"
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateOtp(@RequestBody OtpRequestDto request) {
        try {
            Map<String, String> response = new HashMap<>();
            
            if (request.getOtpType() == null || request.getOtpType().isEmpty()) {
                response.put("error", "OTP type is required. Use EMAIL or SMS");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String message = otpService.generateAndSendOtp(request);
            response.put("success", message);
            response.put("message", "OTP sent successfully. Valid for 5 minutes.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to generate OTP: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verify OTP
     * Request: {
     *   "email": "user@example.com",
     *   "phoneNumber": "9555660256",
     *   "otp": "123456"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody OtpVerificationDto verification) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            if (verification.getOtp() == null || verification.getOtp().isEmpty()) {
                response.put("error", "OTP is required");
                response.put("verified", false);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean isVerified = otpService.verifyOtp(verification);
            response.put("verified", isVerified);
            response.put("message", "OTP verified successfully!");
            response.put("success", true);
            
            // Add the verified identifier for next step (login)
            if (verification.getEmail() != null) {
                response.put("identifier", verification.getEmail());
                response.put("type", "EMAIL");
            } else if (verification.getPhoneNumber() != null) {
                response.put("identifier", verification.getPhoneNumber());
                response.put("type", "SMS");
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("verified", false);
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "OTP verification failed: " + e.getMessage());
            response.put("verified", false);
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OTP Service is running");
        response.put("service", "Authentication Service");
        return ResponseEntity.ok(response);
    }
}

