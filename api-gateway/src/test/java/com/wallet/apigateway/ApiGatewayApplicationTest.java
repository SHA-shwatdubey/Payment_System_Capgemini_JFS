package com.wallet.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayApplicationTest {

    @Test
    void applicationClass_hasSpringBootAnnotation() {
        assertThat(ApiGatewayApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = ApiGatewayApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

