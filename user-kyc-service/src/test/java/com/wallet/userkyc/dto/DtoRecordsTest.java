package com.wallet.userkyc.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void kycStatusRequest_recordContract() {
        KycStatusRequest request1 = new KycStatusRequest("APPROVED", "ok");
        KycStatusRequest request2 = new KycStatusRequest("APPROVED", "ok");

        assertThat(request1.status()).isEqualTo("APPROVED");
        assertThat(request1.reason()).isEqualTo("ok");
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.toString()).contains("APPROVED");
    }

    @Test
    void kycVerifyRequest_recordContract() {
        KycVerifyRequest request = new KycVerifyRequest(77L, "DOC-77");

        assertThat(request.userId()).isEqualTo(77L);
        assertThat(request.documentRef()).isEqualTo("DOC-77");
        assertThat(request.toString()).contains("DOC-77");
    }

    @Test
    void kycVerifyResponse_recordContract() {
        KycVerifyResponse response = new KycVerifyResponse("ref-1", "VERIFIED", "done");

        assertThat(response.verificationRef()).isEqualTo("ref-1");
        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(response.providerMessage()).isEqualTo("done");
    }

    @Test
    void notificationEvent_recordContract() {
        NotificationEvent event1 = new NotificationEvent(5L, "KYC_STATUS_UPDATE", "EMAIL", "x@y.com", "approved");
        NotificationEvent event2 = new NotificationEvent(5L, "KYC_STATUS_UPDATE", "EMAIL", "x@y.com", "approved");

        assertThat(event1.userId()).isEqualTo(5L);
        assertThat(event1.eventType()).isEqualTo("KYC_STATUS_UPDATE");
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}

