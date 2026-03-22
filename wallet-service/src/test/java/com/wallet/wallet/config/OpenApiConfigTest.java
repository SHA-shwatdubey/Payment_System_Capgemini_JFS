package com.wallet.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void walletServiceOpenAPI_buildsExpectedDefinition() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.walletServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API Gateway - Wallet Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getServers()).hasSize(1);
    }
}

