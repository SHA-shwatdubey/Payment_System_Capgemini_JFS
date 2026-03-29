package com.wallet.integration.service;

import com.wallet.integration.dto.KycVerifyRequest;
import com.wallet.integration.dto.KycVerifyResponse;
import com.wallet.integration.dto.PaymentInitRequest;
import com.wallet.integration.dto.PaymentRefundRequest;
import com.wallet.integration.dto.PaymentStatusUpdateRequest;
import com.wallet.integration.entity.PaymentTransaction;
import com.wallet.integration.repository.KycVerificationRepository;
import com.wallet.integration.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private KycVerificationRepository kycRepository;

    @InjectMocks
    private IntegrationService integrationService;

    @Test
    void initPayment_createsPendingTransaction() {
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = integrationService.initPayment(new PaymentInitRequest(1L, new BigDecimal("50"), "UPI"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.paymentRef()).startsWith("PAY-");
    }

    @Test
    void updatePaymentStatus_whenRefMissing_throwsException() {
        when(paymentRepository.findByPaymentRef("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.updatePaymentStatus("missing",
                new PaymentStatusUpdateRequest("SUCCESS", "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment ref not found");
    }

    @Test
    void refundPayment_whenNotSuccess_throwsException() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentRef("PAY-1");
        tx.setStatus("PENDING");
        when(paymentRepository.findByPaymentRef("PAY-1")).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> integrationService.refundPayment("PAY-1", new PaymentRefundRequest("duplicate")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only successful payments can be refunded");
    }

    @Test
    void verifyKyc_returnsVerifiedStatus() {
        when(kycRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KycVerifyResponse result = integrationService.verifyKyc(new KycVerifyRequest(2L, "doc-1"));

        assertThat(result.status()).isEqualTo("VERIFIED");
        verify(kycRepository).save(any());
    }
}
