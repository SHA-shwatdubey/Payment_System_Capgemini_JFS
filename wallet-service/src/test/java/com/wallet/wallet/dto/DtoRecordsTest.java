package com.wallet.wallet.dto;

import com.wallet.wallet.entity.WalletAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    @Test
    void dtoRecords_coverBasicContract() {
        TopupRequest topupRequest = new TopupRequest(1L, new BigDecimal("100"), "UPI");
        TransferRequest transferRequest = new TransferRequest(2L, 3L, new BigDecimal("25"));
        PaymentTopupInitRequest initRequest = new PaymentTopupInitRequest(4L, new BigDecimal("50"), "CARD");
        PaymentTopupInitResponse initResponse = new PaymentTopupInitResponse("ref-1", "CREATED", "url");
        ExternalPaymentStatus status = new ExternalPaymentStatus("ref-2", 5L, new BigDecimal("70"), "UPI", "SUCCESS");
        WalletLimitUpdateRequest limitUpdate = new WalletLimitUpdateRequest(new BigDecimal("1000"), new BigDecimal("500"), 3);
        NotificationEvent notificationEvent = new NotificationEvent(6L, "WALLET_CREDIT", "SMS", "user-6", "ok");
        WalletEvent walletEvent = new WalletEvent(7L, "TOPUP", new BigDecimal("10"));

        assertThat(topupRequest.paymentMethod()).isEqualTo("UPI");
        assertThat(transferRequest.toUserId()).isEqualTo(3L);
        assertThat(initRequest.method()).isEqualTo("CARD");
        assertThat(initResponse.paymentRef()).isEqualTo("ref-1");
        assertThat(status.status()).isEqualTo("SUCCESS");
        assertThat(limitUpdate.dailyTransferCountLimit()).isEqualTo(3);
        assertThat(notificationEvent.channel()).isEqualTo("SMS");
        assertThat(walletEvent.eventType()).isEqualTo("TOPUP");
    }

    @Test
    void paymentTopupConfirmResponse_exposesWalletAccount() {
        WalletAccount account = new WalletAccount();
        account.setUserId(10L);
        account.setBalance(new BigDecimal("300"));

        PaymentTopupConfirmResponse response = new PaymentTopupConfirmResponse("ref-10", "CAPTURED", account);

        assertThat(response.walletAccount().getUserId()).isEqualTo(10L);
    }
}

