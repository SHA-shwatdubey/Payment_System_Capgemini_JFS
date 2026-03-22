package com.wallet.integration.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtRoleValidatorTest {

    private static final String RAW_SECRET = "01234567890123456789012345678901";
    private static final String BASE64_SECRET = Encoders.BASE64.encode(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void extractRole_returnsUppercaseRole() {
        SecretKey key = Keys.hmacShaKeyFor(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().claim("role", "admin").signWith(key).compact();

        JwtRoleValidator validator = new JwtRoleValidator(BASE64_SECRET);
        String role = validator.extractRole(token);

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_withBlankToken_throwsJwtException() {
        JwtRoleValidator validator = new JwtRoleValidator(BASE64_SECRET);

        assertThatThrownBy(() -> validator.extractRole(" "))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void constructor_withBlankSecret_throwsIllegalStateException() {
        assertThatThrownBy(() -> new JwtRoleValidator(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.jwt.secret");
    }
}

