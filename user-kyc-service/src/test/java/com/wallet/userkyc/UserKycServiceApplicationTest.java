package com.wallet.userkyc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserKycServiceApplicationTest {

    @Test
    @DisplayName("Application class keeps expected bootstrap annotations")
    void applicationClass_hasExpectedAnnotations() {
        assertThat(UserKycServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(UserKycServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = UserKycServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}


