package com.wallet.rewards.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void rewardsServiceOpenAPI_buildsExpectedDefinition() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.rewardsServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API Gateway - Rewards Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getServers()).hasSize(1);
    }
}

