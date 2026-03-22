package com.wallet.userkyc.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void userKycServiceOpenAPI_buildsExpectedMetadataAndSecurity() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.userKycServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API Gateway - User KYC Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("/");
    }
}

