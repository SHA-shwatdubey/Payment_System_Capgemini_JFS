package com.wallet.userkyc.controller;

import com.wallet.userkyc.dto.KycStatusRequest;
import com.wallet.userkyc.entity.UserProfile;
import com.wallet.userkyc.security.JwtRoleValidator;
import com.wallet.userkyc.service.UserKycService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping
@Validated
public class UserKycController {

    private final UserKycService service;
    private final JwtRoleValidator jwtRoleValidator;

    public UserKycController(UserKycService service, JwtRoleValidator jwtRoleValidator) {
        this.service = service;
        this.jwtRoleValidator = jwtRoleValidator;
    }

    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile createUser(@Valid @RequestBody UserProfile user) {
        return service.createUser(user);
    }

    @GetMapping("/api/users/{id}")
    public UserProfile getUser(@PathVariable("id") @Positive(message = "id must be positive") Long id) {
        return service.getUser(id);
    }

    @GetMapping("/api/users/lookup")
    public UserProfile lookupUser(@RequestParam("q") String identifier) {
        return service.lookupUser(identifier);
    }

    @GetMapping("/api/support/users/{id}")
    public UserProfile supportGetUser(@PathVariable("id") @Positive(message = "id must be positive") Long id) {
        return service.getUser(id);
    }

    @PostMapping("/api/kyc/submit/{userId}")
    public UserProfile submitKyc(
            @PathVariable("userId") @Positive(message = "userId must be positive") Long userId,
            @RequestParam("documentId") String documentId
    ) {
        return service.submitKyc(userId, documentId);
    }

    @PostMapping(value = "/api/kyc/upload/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfile submitKycWithFile(
            @PathVariable("userId") @Positive(message = "userId must be positive") Long userId,
            @RequestParam("document") MultipartFile document,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone
    ) {
        return service.submitKycFile(userId, document, fullName, email, phone);
    }

    @PutMapping("/api/kyc/{userId}/status")
    public UserProfile updateStatus(
            @PathVariable("userId") @Positive(message = "userId must be positive") Long userId,
            @Valid @RequestBody KycStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        ensureAdmin(httpRequest);
        return service.updateKycStatus(userId, request);
    }

    @GetMapping("/api/kyc/pending")
    public List<UserProfile> pendingKyc(HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        return service.pendingKyc();
    }

    private void ensureAdmin(HttpServletRequest request) {
        String roleHeader = request.getHeader("X-Authenticated-Role");
        if ("ADMIN".equalsIgnoreCase(roleHeader)) {
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String role = jwtRoleValidator.extractRole(token);
                if ("ADMIN".equalsIgnoreCase(role)) {
                    return;
                }
            } catch (JwtException ignored) {
                // Fall through to forbidden response.
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can change or review KYC status");
    }
}
