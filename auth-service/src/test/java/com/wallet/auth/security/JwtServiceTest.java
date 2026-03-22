package com.wallet.auth.security;

import com.wallet.auth.entity.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generateToken_containsExpectedClaims() {
        String secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        JwtService jwtService = new JwtService(secret, 60000);
        AuthUser user = new AuthUser(99L, "alice", "encoded", "SUPPORT");

        String token = jwtService.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("SUPPORT");
        assertThat(claims.get("userId", Long.class)).isEqualTo(99L);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}

