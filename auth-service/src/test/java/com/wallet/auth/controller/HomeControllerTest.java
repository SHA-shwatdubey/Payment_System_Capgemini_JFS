package com.wallet.auth.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void home_returnsServiceLinks() {
        HomeController controller = new HomeController();

        Map<String, String> response = controller.home();

        assertThat(response).containsEntry("service", "auth-service");
        assertThat(response).containsEntry("swagger", "/swagger-ui.html");
        assertThat(response).containsEntry("apiDocs", "/v3/api-docs");
    }
}

