package com.wallet.auth.service;

import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.entity.AuthUser;
import com.wallet.auth.repository.AuthUserRepository;
import com.wallet.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String ROLE_USER = "USER";

    private final AuthUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthUserRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(AuthRequest request) {
        repository.findByUsername(request.username()).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists");
        });

        String role = normalizeSignupRole(request.role());
        AuthUser user = new AuthUser(null, request.username(), passwordEncoder.encode(request.password()), role);
        repository.save(user);
        return new AuthResponse(null, user.getRole(), "Signup successful. Please login to get token");
    }

    public AuthResponse login(AuthRequest request) {
        AuthUser user = repository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String storedPassword = user.getPassword();
        boolean valid = isValidPassword(request.password(), storedPassword);
        if (!valid) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Auto-migrate legacy plain-text password to bcrypt hash on successful login.
        if (!isBcryptHash(storedPassword)) {
            user.setPassword(passwordEncoder.encode(request.password()));
            repository.save(user);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getRole(), "Login successful");
    }

    private boolean isValidPassword(String rawPassword, String storedPassword) {
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.startsWith("$2");
    }

    private String normalizeSignupRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }

        String normalized = role.trim().toUpperCase();
        if (ROLE_USER.equals(normalized) || "SUPPORT".equals(normalized) || "MERCHANT".equals(normalized)) {
            return normalized;
        }

        throw new IllegalArgumentException("Unsupported role. Allowed: USER, SUPPORT, MERCHANT");
    }
}

