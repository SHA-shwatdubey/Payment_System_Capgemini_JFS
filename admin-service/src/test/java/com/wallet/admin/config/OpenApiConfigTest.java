package com.wallet.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void adminServiceOpenAPI_buildsExpectedDefinition() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.adminServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API Gateway - Admin Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getServers()).hasSize(1);
    }
}

