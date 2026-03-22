package com.wallet.integration.service;

import com.wallet.integration.dto.KycVerifyRequest;
import com.wallet.integration.dto.KycVerifyResponse;
import com.wallet.integration.dto.PaymentInitRequest;
import com.wallet.integration.dto.PaymentInitResponse;
import com.wallet.integration.dto.PaymentRefundRequest;
import com.wallet.integration.dto.PaymentStatusResponse;
import com.wallet.integration.dto.PaymentStatusUpdateRequest;
import com.wallet.integration.entity.KycVerification;
import com.wallet.integration.entity.PaymentTransaction;
import com.wallet.integration.repository.KycVerificationRepository;
import com.wallet.integration.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IntegrationService {

    private static final String ERROR_PAYMENT_REF_NOT_FOUND = "Payment ref not found";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_REFUNDED = "REFUNDED";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final PaymentTransactionRepository paymentRepository;
    private final KycVerificationRepository kycRepository;

    public IntegrationService(PaymentTransactionRepository paymentRepository,
                              KycVerificationRepository kycRepository) {
        this.paymentRepository = paymentRepository;
        this.kycRepository = kycRepository;
    }

    @Transactional
    public PaymentInitResponse initPayment(PaymentInitRequest request) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setPaymentRef("PAY-" + UUID.randomUUID());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(STATUS_PENDING);
        paymentRepository.save(payment);

        String paymentUrl = "https://mock-payments.local/checkout/" + payment.getPaymentRef();
        return new PaymentInitResponse(payment.getPaymentRef(), payment.getStatus(), paymentUrl);
    }

    @Transactional
    public PaymentTransaction updatePaymentStatus(String paymentRef, PaymentStatusUpdateRequest request) {
        PaymentTransaction payment = paymentRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_PAYMENT_REF_NOT_FOUND));
        payment.setStatus(request.status());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse paymentStatus(String paymentRef) {
        PaymentTransaction payment = paymentRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_PAYMENT_REF_NOT_FOUND));
        return new PaymentStatusResponse(
                payment.getPaymentRef(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus()
        );
    }

    @Transactional
    public PaymentTransaction refundPayment(String paymentRef, PaymentRefundRequest request) {
        PaymentTransaction payment = paymentRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_PAYMENT_REF_NOT_FOUND));
        if (!STATUS_SUCCESS.equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalArgumentException("Only successful payments can be refunded");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Refund reason is required");
        }
        payment.setStatus(STATUS_REFUNDED);
        return paymentRepository.save(payment);
    }

    @Transactional
    public KycVerifyResponse verifyKyc(KycVerifyRequest request) {
        KycVerification verification = new KycVerification();
        verification.setUserId(request.userId());
        verification.setVerificationRef("KYC-" + UUID.randomUUID());
        verification.setStatus(STATUS_VERIFIED);
        verification.setProviderMessage("Document verified by mock provider");
        kycRepository.save(verification);

        return new KycVerifyResponse(
                verification.getVerificationRef(),
                verification.getStatus(),
                verification.getProviderMessage()
        );
    }
}



