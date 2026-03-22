package com.wallet.transaction.dto;

import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void dtoRecords_coverBasicContract() {
        TopupRequest topupRequest = new TopupRequest(1L, new BigDecimal("100"), "idem-1");
        TransferRequest transferRequest = new TransferRequest(2L, 3L, new BigDecimal("50"), "idem-2");
        PaymentRequest paymentRequest = new PaymentRequest(4L, 5L, new BigDecimal("60"), "idem-3");
        RefundRequest refundRequest = new RefundRequest(6L, 7L, new BigDecimal("20"), 9L, "idem-4");
        WalletEvent walletEvent = new WalletEvent(8L, "TOPUP", new BigDecimal("10"));
        TransactionResponse response = new TransactionResponse(
                11L, 1L, 0L, 1L, new BigDecimal("75"), TransactionType.TOPUP,
                TransactionStatus.SUCCESS, "idem-5", LocalDateTime.now()
        );

        assertThat(topupRequest.idempotencyKey()).isEqualTo("idem-1");
        assertThat(transferRequest.receiverId()).isEqualTo(3L);
        assertThat(paymentRequest.amount()).isEqualByComparingTo("60");
        assertThat(refundRequest.originalTransactionId()).isEqualTo(9L);
        assertThat(walletEvent.eventType()).isEqualTo("TOPUP");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
    }
}

