package com.wallet.notification.controller;

import com.wallet.notification.dto.DeviceTokenRequest;
import com.wallet.notification.dto.NotificationSendRequest;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.entity.DeviceToken;
import com.wallet.notification.entity.NotificationMessage;
import com.wallet.notification.security.JwtRoleValidator;
import com.wallet.notification.service.NotificationService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    private final NotificationService notificationService;
    private final JwtRoleValidator jwtRoleValidator;

    public NotificationController(NotificationService notificationService,
                                  JwtRoleValidator jwtRoleValidator) {
        this.notificationService = notificationService;
        this.jwtRoleValidator = jwtRoleValidator;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationMessage send(@RequestBody NotificationSendRequest request,
                                    HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return notificationService.send(request);
    }

    @GetMapping("/history")
    public List<NotificationMessage> history(@RequestParam("userId") Long userId,
                                             HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return notificationService.history(userId);
    }

    @PostMapping("/device-token")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceToken registerDevice(@RequestBody DeviceTokenRequest request,
                                      HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return notificationService.registerDevice(request);
    }

    @GetMapping("/admin/stats")
    public NotificationStatsResponse stats(HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        return notificationService.stats();
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
                // Fall through to forbidden response.
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
