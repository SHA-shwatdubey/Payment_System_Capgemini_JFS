package com.wallet.userkyc.dto;

public record KycVerifyResponse(String verificationRef, String status, String providerMessage) {
}

