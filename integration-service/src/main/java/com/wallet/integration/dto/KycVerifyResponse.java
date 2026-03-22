package com.wallet.integration.dto;

public record KycVerifyResponse(String verificationRef, String status, String providerMessage) {
}

