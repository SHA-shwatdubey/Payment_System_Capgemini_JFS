package com.wallet.integration.controller;

import com.wallet.integration.dto.KycVerifyRequest;
import com.wallet.integration.dto.KycVerifyResponse;
import com.wallet.integration.dto.PaymentInitRequest;
import com.wallet.integration.dto.PaymentInitResponse;
import com.wallet.integration.dto.PaymentRefundRequest;
import com.wallet.integration.dto.PaymentStatusResponse;
import com.wallet.integration.dto.PaymentStatusUpdateRequest;
import com.wallet.integration.entity.PaymentTransaction;
import com.wallet.integration.security.JwtRoleValidator;
import com.wallet.integration.service.IntegrationService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    private final IntegrationService integrationService;
    private final JwtRoleValidator jwtRoleValidator;

    public IntegrationController(IntegrationService integrationService,
                                 JwtRoleValidator jwtRoleValidator) {
        this.integrationService = integrationService;
        this.jwtRoleValidator = jwtRoleValidator;
    }

    @PostMapping("/payments/init")
    public PaymentInitResponse initPayment(@RequestBody PaymentInitRequest request,
                                           HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return integrationService.initPayment(request);
    }

    @PutMapping("/payments/{paymentRef}/status")
    public PaymentTransaction updatePaymentStatus(@PathVariable("paymentRef") String paymentRef,
                                                  @RequestBody PaymentStatusUpdateRequest request,
                                                  HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        return integrationService.updatePaymentStatus(paymentRef, request);
    }

    @GetMapping("/payments/{paymentRef}")
    public PaymentStatusResponse paymentStatus(@PathVariable("paymentRef") String paymentRef,
                                               HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return integrationService.paymentStatus(paymentRef);
    }

    @PostMapping("/payments/{paymentRef}/refund")
    public PaymentTransaction refundPayment(@PathVariable("paymentRef") String paymentRef,
                                            @RequestBody PaymentRefundRequest request,
                                            HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return integrationService.refundPayment(paymentRef, request);
    }

    @PostMapping("/kyc/verify")
    public KycVerifyResponse verifyKyc(@RequestBody KycVerifyRequest request,
                                       HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return integrationService.verifyKyc(request);
    }

    private void ensureAuthenticated(HttpServletRequest request) {
        if (isInternalCall(request)) {
            return;
        }

        String roleHeader = request.getHeader("X-Authenticated-Role");
        if (roleHeader != null && !roleHeader.isBlank()) {
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String role = jwtRoleValidator.extractRole(token);
                if (!role.isBlank()) {
                    return;
                }
            } catch (JwtException ignored) {
                // Fall through to unauthorized response.
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
    }

    private void ensureAdmin(HttpServletRequest request) {
        if (isInternalCall(request)) {
            return;
        }

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

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can access this endpoint");
    }

    private boolean isInternalCall(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader(INTERNAL_CALL_HEADER));
    }
}
