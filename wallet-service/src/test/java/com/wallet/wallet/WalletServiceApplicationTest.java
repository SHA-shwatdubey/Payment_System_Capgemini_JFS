package com.wallet.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WalletServiceApplicationTest {

    @Test
    void applicationClass_hasExpectedAnnotations() {
        assertThat(WalletServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(WalletServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }

    @Test
    void mainMethod_exists() throws NoSuchMethodException {
        Method method = WalletServiceApplication.class.getMethod("main", String[].class);
        assertThat(method).isNotNull();
    }
}

