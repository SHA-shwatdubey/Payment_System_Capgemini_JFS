package com.wallet.apigateway;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SwaggerRedirectController {
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "api-gateway",
                "swagger", "/swagger-ui.html",
                "apiDocs", "/v3/api-docs"
        );
    }

    @GetMapping("/swagger-ui/index.html")
    public ResponseEntity<Void> swaggerIndex() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/swagger-ui.html");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}

