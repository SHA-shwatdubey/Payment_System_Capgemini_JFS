package com.wallet.auth.dto;

public record AuthResponse(String token, String role, String message) {
}

