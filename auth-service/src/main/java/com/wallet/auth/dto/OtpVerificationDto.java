package com.wallet.auth.dto;

public class OtpVerificationDto {
    private String email;
    private String phoneNumber;
    private String otp;

    public OtpVerificationDto() {
    }

    public OtpVerificationDto(String email, String phoneNumber, String otp) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

