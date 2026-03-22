package com.wallet.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionServiceApplicationTest {

    @Test
    void applicationClass_hasExpectedAnnotations() {
        assertThat(TransactionServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(TransactionServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = TransactionServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

