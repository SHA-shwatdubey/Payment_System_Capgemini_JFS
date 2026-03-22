package com.wallet.auth.service;

import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;
import com.wallet.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_withValidRequest_returnsUserRole() {
        AuthRequest request = new AuthRequest("alice", "secret", "merchant");
        when(repository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        AuthResponse response = authService.signup(request);

        assertThat(response.role()).isEqualTo("MERCHANT");
        verify(repository).save(any(AuthUser.class));
    }

    @Test
    void signup_withNullRole_defaultsToUser() {
        AuthRequest request = new AuthRequest("alice", "secret", null);
        when(repository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        AuthResponse response = authService.signup(request);

        assertThat(response.role()).isEqualTo("USER");
        verify(repository).save(any(AuthUser.class));
    }

    @Test
    void signup_withUnsupportedRole_throwsException() {
        AuthRequest request = new AuthRequest("alice", "secret", "admin");
        when(repository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported role");
    }

    @Test
    void signup_withExistingUsername_throwsException() {
        when(repository.findByUsername("alice")).thenReturn(Optional.of(new AuthUser()));

        assertThatThrownBy(() -> authService.signup(new AuthRequest("alice", "x", "USER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void login_withWrongPassword_throwsException() {
        AuthUser user = new AuthUser(1L, "alice", "$2a$10$hash", "USER");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new AuthRequest("alice", "wrong", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_withUnknownUser_throwsException() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthRequest("missing", "secret", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_withValidPassword_returnsToken() {
        AuthUser user = new AuthUser(1L, "alice", "$2a$10$hash", "USER");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt");

        AuthResponse response = authService.login(new AuthRequest("alice", "secret", null));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.role()).isEqualTo("USER");
        verify(repository, never()).save(any(AuthUser.class));
    }

    @Test
    void login_withLegacyPlainTextPassword_migratesPasswordHash() {
        AuthUser user = new AuthUser(1L, "alice", "legacy-pass", "USER");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("legacy-pass")).thenReturn("$2a$10$newhash");
        when(jwtService.generateToken(user)).thenReturn("jwt");

        AuthResponse response = authService.login(new AuthRequest("alice", "legacy-pass", null));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(user.getPassword()).isEqualTo("$2a$10$newhash");
        verify(repository).save(user);
    }
}


