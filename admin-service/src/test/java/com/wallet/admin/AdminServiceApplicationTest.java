package com.wallet.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AdminServiceApplicationTest {

    @Test
    void applicationClass_hasExpectedAnnotations() {
        assertThat(AdminServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(AdminServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = AdminServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}


