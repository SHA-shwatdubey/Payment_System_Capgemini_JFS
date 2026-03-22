package com.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationServiceApplicationTest {

    @Test
    void applicationClass_hasSpringBootAnnotation() {
        assertThat(IntegrationServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = IntegrationServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

