package com.wallet.integration.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void paymentRecords_coverRecordContract() {
        PaymentInitRequest initRequest = new PaymentInitRequest(1L, new BigDecimal("50.00"), "UPI");
        PaymentInitResponse initResponse = new PaymentInitResponse("PAY-1", "PENDING", "https://mock/1");
        PaymentStatusUpdateRequest statusUpdate = new PaymentStatusUpdateRequest("SUCCESS", "ok");
        PaymentStatusResponse statusResponse = new PaymentStatusResponse("PAY-1", 1L, new BigDecimal("50.00"), "UPI", "SUCCESS");
        PaymentRefundRequest refundRequest = new PaymentRefundRequest("duplicate");

        assertThat(initRequest.userId()).isEqualTo(1L);
        assertThat(initRequest.method()).isEqualTo("UPI");
        assertThat(initResponse.paymentRef()).isEqualTo("PAY-1");
        assertThat(statusUpdate.status()).isEqualTo("SUCCESS");
        assertThat(statusResponse.status()).isEqualTo("SUCCESS");
        assertThat(refundRequest.reason()).isEqualTo("duplicate");
    }

    @Test
    void kycRecords_coverRecordContract() {
        KycVerifyRequest request = new KycVerifyRequest(7L, "DOC-7");
        KycVerifyResponse response1 = new KycVerifyResponse("KYC-1", "VERIFIED", "ok");
        KycVerifyResponse response2 = new KycVerifyResponse("KYC-1", "VERIFIED", "ok");

        assertThat(request.documentRef()).isEqualTo("DOC-7");
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).contains("VERIFIED");
    }
}

