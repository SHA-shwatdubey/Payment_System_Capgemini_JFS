package com.wallet.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = SwaggerRedirectController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class SwaggerRedirectControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void home_returnsGatewayMetadata() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("api-gateway")
                .jsonPath("$.swagger").isEqualTo("/swagger-ui.html")
                .jsonPath("$.apiDocs").isEqualTo("/v3/api-docs");
    }

    @Test
    void swaggerIndex_redirectsToSwaggerUiHtml() {
        webTestClient.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "/swagger-ui.html");
    }
}

