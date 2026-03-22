package com.wallet.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServerApplicationTest {

    @Test
    void applicationClass_hasExpectedAnnotations() {
        assertThat(ConfigServerApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(ConfigServerApplication.class.isAnnotationPresent(EnableConfigServer.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = ConfigServerApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

