package com.wallet.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret is required");
        }
        SecretKey key;
        try {
            key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (Exception ex) {
            // Allow raw secrets from local env files while keeping strict validation for short/invalid values.
            byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (raw.length >= 32) {
                key = Keys.hmacShaKeyFor(raw);
            } else {
                throw new IllegalStateException("Illegal base64 in security.jwt.secret", ex);
            }
        }
        this.signingKey = key;
    }

    public Claims validateAndGetClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("JWT token is missing");
        }
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid JWT token", ex);
        }
    }
}
