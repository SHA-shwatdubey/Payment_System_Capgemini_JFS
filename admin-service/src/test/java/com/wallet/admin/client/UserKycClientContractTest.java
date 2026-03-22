package com.wallet.admin.client;

import com.wallet.admin.config.FeignAuthForwardingConfig;
import com.wallet.admin.dto.KycApprovalRequest;
import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserKycClientContractTest {

    @Test
    void feignClientAnnotation_hasExpectedNameAndConfig() {
        FeignClient feignClient = UserKycClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("user-kyc-service");
        assertThat(feignClient.configuration()).contains(FeignAuthForwardingConfig.class);
    }

    @Test
    void methods_haveExpectedMappings() throws NoSuchMethodException {
        Method pendingKyc = UserKycClient.class.getMethod("pendingKyc");
        GetMapping getMapping = pendingKyc.getAnnotation(GetMapping.class);
        assertThat(getMapping.value()).containsExactly("/api/kyc/pending");

        Method updateStatus = UserKycClient.class.getMethod("updateStatus", Long.class, KycApprovalRequest.class);
        PutMapping putMapping = updateStatus.getAnnotation(PutMapping.class);
        assertThat(putMapping.value()).containsExactly("/api/kyc/{userId}/status");
        assertThat(updateStatus.getParameterAnnotations()[0][0]).isInstanceOf(PathVariable.class);
        assertThat(updateStatus.getParameterAnnotations()[1][0]).isInstanceOf(RequestBody.class);
    }
}

