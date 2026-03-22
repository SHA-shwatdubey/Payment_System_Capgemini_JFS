package com.wallet.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceApplicationTest {

    @Test
    void applicationClass_hasSpringBootAnnotation() {
        assertThat(NotificationServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = NotificationServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

