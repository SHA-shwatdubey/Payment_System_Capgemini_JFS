package com.wallet.integration.service;

import com.wallet.integration.dto.KycVerifyRequest;
import com.wallet.integration.dto.KycVerifyResponse;
import com.wallet.integration.dto.PaymentInitRequest;
import com.wallet.integration.dto.PaymentInitResponse;
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
    void initPayment_createsTransactionAndReturnsRef() {
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitResponse response = integrationService.initPayment(new PaymentInitRequest(1L, new BigDecimal("100.00"), "UPI"));

        assertThat(response.paymentRef()).startsWith("PAY-");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(paymentRepository).save(any());
    }

    @Test
    void updatePaymentStatus_whenRefFound_updatesStatus() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentRef("PAY-1");
        when(paymentRepository.findByPaymentRef("PAY-1")).thenReturn(Optional.of(tx));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction result = integrationService.updatePaymentStatus("PAY-1", new PaymentStatusUpdateRequest("SUCCESS", "Paid"));

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository).save(any());
    }

    @Test
    void refundPayment_whenSuccessful_marksRefunded() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentRef("PAY-SUCCESS");
        tx.setStatus("SUCCESS");
        when(paymentRepository.findByPaymentRef("PAY-SUCCESS")).thenReturn(Optional.of(tx));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction result = integrationService.refundPayment("PAY-SUCCESS", new PaymentRefundRequest("Fraud"));

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void refundPayment_whenNotSuccessful_throwsException() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus("PENDING");
        when(paymentRepository.findByPaymentRef("PAY-P")).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> integrationService.refundPayment("PAY-P", new PaymentRefundRequest("Reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only successful payments");
    }

    @Test
    void verifyKyc_returnsVerifiedStatus() {
        when(kycRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KycVerifyResponse result = integrationService.verifyKyc(new KycVerifyRequest(2L, "DOC-123"));

        assertThat(result.status()).isEqualTo("VERIFIED");
        verify(kycRepository).save(any());
    }
}
