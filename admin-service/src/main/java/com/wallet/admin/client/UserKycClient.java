package com.wallet.admin.client;
// package 
import com.wallet.admin.config.FeignAuthForwardingConfig;
import com.wallet.admin.dto.KycApprovalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-kyc-service", configuration = FeignAuthForwardingConfig.class)
public interface UserKycClient {
    @GetMapping("/api/kyc/pending")
    List<Object> pendingKyc();

    @PutMapping("/api/kyc/{userId}/status")
    Object updateStatus(@PathVariable("userId") Long userId, @RequestBody KycApprovalRequest request);
}
