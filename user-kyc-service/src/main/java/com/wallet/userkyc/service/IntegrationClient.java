package com.wallet.userkyc.service;

import com.wallet.userkyc.dto.KycVerifyRequest;
import com.wallet.userkyc.dto.KycVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "integration-service")
public interface IntegrationClient {

    String INTERNAL_CALL_HEADER = "X-Internal-Call";
    String INTERNAL_CALL_VALUE = "true";

    @PostMapping("/api/integrations/kyc/verify")
    KycVerifyResponse verifyKyc(@RequestHeader(INTERNAL_CALL_HEADER) String internalCall,
                                @RequestBody KycVerifyRequest request);
}


