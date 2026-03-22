package com.wallet.configserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ConfigServerController {
    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "config-server",
                "status", "UP",
                "endpoints", Map.of(
                        "health", "/actuator/health",
                        "sampleConfig", "/admin-service/default",
                        "gatewayConfig", "/api-gateway/default"
                )
        );
    }
}

