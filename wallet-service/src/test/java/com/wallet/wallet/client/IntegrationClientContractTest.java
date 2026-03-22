package com.wallet.wallet.client;

import com.wallet.wallet.dto.ExternalPaymentStatus;
import com.wallet.wallet.dto.ExternalPaymentStatusUpdateRequest;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    void initPayment_contractAnnotationsArePresent() throws NoSuchMethodException {
        Method method = IntegrationClient.class.getMethod("initPayment", String.class, PaymentTopupInitRequest.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertThat(postMapping.value()).containsExactly("/api/integrations/payments/init");
        assertThat(method.getParameterAnnotations()[0][0]).isInstanceOf(RequestHeader.class);
        assertThat(method.getParameterAnnotations()[1][0]).isInstanceOf(RequestBody.class);
        assertThat(method.getReturnType()).isEqualTo(PaymentTopupInitResponse.class);
    }

    @Test
    void paymentStatus_contractAnnotationsArePresent() throws NoSuchMethodException {
        Method method = IntegrationClient.class.getMethod("paymentStatus", String.class, String.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping.value()).containsExactly("/api/integrations/payments/{paymentRef}");
        assertThat(method.getParameterAnnotations()[0][0]).isInstanceOf(RequestHeader.class);
        assertThat(method.getParameterAnnotations()[1][0]).isInstanceOf(PathVariable.class);
        assertThat(method.getReturnType()).isEqualTo(ExternalPaymentStatus.class);
    }

    @Test
    void updatePaymentStatus_contractAnnotationsArePresent() throws NoSuchMethodException {
        Method method = IntegrationClient.class.getMethod(
                "updatePaymentStatus", String.class, String.class, ExternalPaymentStatusUpdateRequest.class
        );

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        assertThat(putMapping.value()).containsExactly("/api/integrations/payments/{paymentRef}/status");
        assertThat(method.getParameterAnnotations()[0][0]).isInstanceOf(RequestHeader.class);
        assertThat(method.getParameterAnnotations()[1][0]).isInstanceOf(PathVariable.class);
        assertThat(method.getParameterAnnotations()[2][0]).isInstanceOf(RequestBody.class);
    }
}

