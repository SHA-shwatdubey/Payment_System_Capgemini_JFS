package com.wallet.userkyc.service;

import com.wallet.userkyc.dto.KycVerifyRequest;
import com.wallet.userkyc.dto.KycVerifyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationClientContractTest {

    @Test
    void feignClientAnnotation_hasExpectedServiceName() {
        FeignClient feignClient = IntegrationClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("integration-service");
    }

    @Test
    void verifyKyc_contractAnnotationsArePresent() throws NoSuchMethodException {
        Method method = IntegrationClient.class.getMethod(
                "verifyKyc", String.class, KycVerifyRequest.class
        );

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/api/integrations/kyc/verify");

        assertThat(method.getParameterAnnotations()[0][0]).isInstanceOf(RequestHeader.class);
        assertThat(method.getParameterAnnotations()[1][0]).isInstanceOf(RequestBody.class);
        assertThat(method.getReturnType()).isEqualTo(KycVerifyResponse.class);
    }
}

