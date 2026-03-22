package com.wallet.userkyc.security;

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

    private static final String SECRET = Encoders.BASE64.encode("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void extractRole_returnsUppercaseRoleFromJwt() {
        SecretKey key = Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().claim("role", "admin").signWith(key).compact();

        JwtRoleValidator validator = new JwtRoleValidator(SECRET);
        String role = validator.extractRole(token);

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_withBlankToken_throwsJwtException() {
        JwtRoleValidator validator = new JwtRoleValidator(SECRET);

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

