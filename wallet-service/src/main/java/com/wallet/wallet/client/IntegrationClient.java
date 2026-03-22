package com.wallet.wallet.client;

import com.wallet.wallet.dto.ExternalPaymentStatus;
import com.wallet.wallet.dto.ExternalPaymentStatusUpdateRequest;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "integration-service")
public interface IntegrationClient {

    String INTERNAL_CALL_HEADER = "X-Internal-Call";
    String INTERNAL_CALL_VALUE = "true";

    @PostMapping("/api/integrations/payments/init")
    PaymentTopupInitResponse initPayment(@RequestHeader(INTERNAL_CALL_HEADER) String internalCall,
                                         @RequestBody PaymentTopupInitRequest request);

    @GetMapping("/api/integrations/payments/{paymentRef}")
    ExternalPaymentStatus paymentStatus(@RequestHeader(INTERNAL_CALL_HEADER) String internalCall,
                                        @PathVariable("paymentRef") String paymentRef);

    @PutMapping("/api/integrations/payments/{paymentRef}/status")
    void updatePaymentStatus(@RequestHeader(INTERNAL_CALL_HEADER) String internalCall,
                             @PathVariable("paymentRef") String paymentRef,
                             @RequestBody ExternalPaymentStatusUpdateRequest request);
}



