package com.wallet.rewards.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "rewards-service",
                "swagger", "/swagger-ui.html",
                "apiDocs", "/v3/api-docs"
        );
    }
}

