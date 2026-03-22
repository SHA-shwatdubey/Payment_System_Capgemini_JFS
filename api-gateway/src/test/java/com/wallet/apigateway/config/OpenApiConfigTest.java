package com.wallet.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void gatewayOpenAPI_buildsExpectedMetadataAndSecurity() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.gatewayOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API Gateway");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }
}

