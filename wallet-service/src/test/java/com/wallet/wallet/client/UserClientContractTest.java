package com.wallet.wallet.client;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserClientContractTest {

    @Test
    void feignClientAnnotation_hasExpectedServiceName() {
        FeignClient feignClient = UserClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("user-kyc-service");
    }

    @Test
    void getUserById_contractAnnotationsArePresent() throws NoSuchMethodException {
        Method method = UserClient.class.getMethod("getUserById", Long.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping.value()).containsExactly("/api/users/{id}");
        assertThat(method.getParameterAnnotations()[0][0]).isInstanceOf(PathVariable.class);
    }
}

