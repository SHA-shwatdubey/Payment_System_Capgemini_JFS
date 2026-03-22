package com.wallet.transaction.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "transaction-service",
                "swagger", "/swagger-ui.html",
                "docs", "/v3/api-docs"
        );
    }
}

