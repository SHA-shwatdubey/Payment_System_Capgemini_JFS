package com.wallet.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EurekaServerApplicationTest {

    @Test
    void applicationClass_hasExpectedAnnotations() {
        assertThat(EurekaServerApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(EurekaServerApplication.class.isAnnotationPresent(EnableEurekaServer.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = EurekaServerApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

