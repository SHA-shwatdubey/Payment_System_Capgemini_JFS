package com.wallet.auth.controller;

import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@RequestBody AuthRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    /**
     * Test Endpoint - Simple health check
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "✅ Auth Service is Running!");
        response.put("service", "Authentication Service");
        response.put("port", "8063");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Service is working perfectly!");
        response.put("endpoints", new String[]{
            "/api/auth/test (GET)",
            "/api/auth/otp/generate (POST)",
            "/api/auth/otp/verify (POST)",
            "/api/auth/otp/health (GET)",
            "/api/auth/signup (POST)",
            "/api/auth/login (POST)"
        });
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint with your email and phone
     */
    @GetMapping("/test/user")
    public ResponseEntity<Map<String, String>> testUser() {
        Map<String, String> response = new HashMap<>();
        response.put("name", "Shashwat");
        response.put("email", "shashwatdtech@gmail.com");
        response.put("phone", "9555660256");
        response.put("status", " Ready for OTP Testing");
        response.put("message", "You can now use /api/auth/otp endpoints");
        return ResponseEntity.ok(response);
    }
}


