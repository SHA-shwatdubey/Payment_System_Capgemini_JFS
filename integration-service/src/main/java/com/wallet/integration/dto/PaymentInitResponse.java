package com.wallet.integration.dto;

public record PaymentInitResponse(String paymentRef, String status, String paymentUrl) {
}

