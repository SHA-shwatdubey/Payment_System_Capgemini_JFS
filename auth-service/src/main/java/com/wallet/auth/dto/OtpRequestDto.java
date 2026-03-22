package com.wallet.auth.dto;

public class OtpRequestDto {
    private String email;
    private String phoneNumber;
    private String otpType; // EMAIL or SMS

    public OtpRequestDto() {
    }

    public OtpRequestDto(String email, String phoneNumber, String otpType) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.otpType = otpType;
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

    public String getOtpType() {
        return otpType;
    }

    public void setOtpType(String otpType) {
        this.otpType = otpType;
    }
}

