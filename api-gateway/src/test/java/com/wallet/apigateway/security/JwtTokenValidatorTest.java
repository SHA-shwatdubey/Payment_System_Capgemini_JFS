package com.wallet.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private static final String RAW_SECRET = "01234567890123456789012345678901";
    private static final String BASE64_SECRET = Encoders.BASE64.encode(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void validateAndGetClaims_withValidToken_returnsClaims() {
        SecretKey key = Keys.hmacShaKeyFor(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .claim("role", "USER")
                .claim("userId", 15)
                .signWith(key)
                .compact();

        JwtTokenValidator validator = new JwtTokenValidator(BASE64_SECRET);
        Claims claims = validator.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void validateAndGetClaims_withBlankToken_throwsJwtException() {
        JwtTokenValidator validator = new JwtTokenValidator(BASE64_SECRET);

        assertThatThrownBy(() -> validator.validateAndGetClaims(" "))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void constructor_withInvalidBase64Secret_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtTokenValidator("not-base64"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Illegal base64");
    }
}


