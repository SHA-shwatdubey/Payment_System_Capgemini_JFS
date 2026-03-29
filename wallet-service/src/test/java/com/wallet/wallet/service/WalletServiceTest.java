package com.wallet.wallet.service;

import com.wallet.wallet.client.IntegrationClient;
import com.wallet.wallet.client.UserClient;
import com.wallet.wallet.dto.ExternalPaymentStatus;
import com.wallet.wallet.dto.PaymentTopupConfirmResponse;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.entity.WalletAccount;
import com.wallet.wallet.entity.WalletLimitConfig;
import com.wallet.wallet.repository.LedgerEntryRepository;
import com.wallet.wallet.repository.WalletAccountRepository;
import com.wallet.wallet.repository.WalletLimitConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import feign.FeignException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

        @Mock
        private WalletAccountRepository walletAccountRepository;

        @Mock
        private LedgerEntryRepository ledgerEntryRepository;

        @Mock
        private WalletEventPublisher eventPublisher;

        @Mock
        private UserClient userClient;

        @Mock
        private WalletLimitConfigRepository walletLimitConfigRepository;

        @Mock
        private NotificationClient notificationClient;

        @Mock
        private IntegrationClient integrationClient;

        @InjectMocks
        private WalletService walletService;

        private void mockDefaultLimitChecks() {
                WalletLimitConfig config = new WalletLimitConfig();
                config.setId(1L);
                config.setDailyTopupLimit(new BigDecimal("10000"));
                config.setDailyTransferLimit(new BigDecimal("5000"));
                config.setDailyTransferCountLimit(3);

                when(walletLimitConfigRepository.findById(1L)).thenReturn(Optional.of(config));
                when(ledgerEntryRepository.sumByUserAndTypeAndCreatedAtBetween(any(), any(), any(), any()))
                                .thenReturn(BigDecimal.ZERO);
        }

        @Test
        void topup_withValidRequest_updatesBalance() {
                mockDefaultLimitChecks();

                WalletAccount account = new WalletAccount();
                account.setUserId(1L);
                account.setBalance(new BigDecimal("100"));

                when(walletAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
                when(walletAccountRepository.save(any(WalletAccount.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                WalletAccount result = walletService.topup(new TopupRequest(1L, new BigDecimal("50"), "UPI"));

                assertThat(result.getBalance()).isEqualByComparingTo("150");
                verify(eventPublisher).publish(any());
        }

        @Test
        void transfer_sameSenderAndReceiver_throwsException() {
                TransferRequest request = new TransferRequest(2L, 2L, new BigDecimal("10"));

                assertThatThrownBy(() -> walletService.transfer(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Sender and receiver cannot be the same");
        }

        @Test
        void history_withNullUserId_throwsException() {
                assertThatThrownBy(() -> walletService.history(null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("User id is required");
        }

        @Test
        void initTopupPayment_delegatesToIntegrationClient() {
                PaymentTopupInitRequest request = new PaymentTopupInitRequest(1L, new BigDecimal("90.00"), "UPI");
                when(integrationClient.initPayment(eq("true"), any(PaymentTopupInitRequest.class)))
                                .thenReturn(new PaymentTopupInitResponse("pay-1", "CREATED", "https://url"));

                PaymentTopupInitResponse response = walletService.initTopupPayment(request);

                assertThat(response.paymentRef()).isEqualTo("pay-1");
        }

        @Test
        void confirmTopupPayment_whenStatusCaptured_returnsExistingAccount() {
                WalletAccount account = new WalletAccount();
                account.setUserId(5L);
                account.setBalance(new BigDecimal("200"));
                when(integrationClient.paymentStatus("true", "pay-2"))
                                .thenReturn(new ExternalPaymentStatus("pay-2", 5L, new BigDecimal("40"), "UPI",
                                                "CAPTURED"));
                when(walletAccountRepository.findByUserId(5L)).thenReturn(Optional.of(account));

                PaymentTopupConfirmResponse response = walletService.confirmTopupPayment("pay-2");

                assertThat(response.paymentStatus()).isEqualTo("CAPTURED");
                assertThat(response.walletAccount().getBalance()).isEqualByComparingTo("200");
        }

        @Test
        void topup_whenDailyLimitExceeded_throwsException() {
                WalletLimitConfig config = new WalletLimitConfig();
                config.setDailyTopupLimit(new BigDecimal("100"));
                when(walletLimitConfigRepository.findById(1L)).thenReturn(Optional.of(config));
                when(ledgerEntryRepository.sumByUserAndTypeAndCreatedAtBetween(any(), any(), any(), any()))
                                .thenReturn(new BigDecimal("90"));

                TopupRequest req = new TopupRequest(1L, new BigDecimal("20"), "CARD");
                assertThatThrownBy(() -> walletService.topup(req))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Daily topup limit exceeded");
        }

        @Test
        void transfer_whenDailyLimitExceeded_throwsException() {
                mockDefaultLimitChecks();
                when(userClient.getUserById(2L)).thenReturn(new Object());
                when(ledgerEntryRepository.sumByUserAndTypeAndCreatedAtBetween(any(), eq("TRANSFER_DEBIT"), any(),
                                any()))
                                .thenReturn(new BigDecimal("-4995")); // Limit is 5000

                WalletAccount from = new WalletAccount();
                from.setBalance(new BigDecimal("1000"));
                when(walletAccountRepository.findByUserId(1L)).thenReturn(Optional.of(from));

                TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("10"));
                assertThatThrownBy(() -> walletService.transfer(req))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Daily transfer limit exceeded");
        }

    @Test
    void transfer_whenDailyCountLimitExceeded_throwsException() {
        mockDefaultLimitChecks();
        when(userClient.getUserById(2L)).thenReturn(new Object());
        when(ledgerEntryRepository.countByUserIdAndTypeAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(3L); // Limit is 3

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("10"));
        assertThatThrownBy(() -> walletService.transfer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer count limit exceeded");
    }

    @Test
    void confirmTopupPayment_whenFeignNotFound_throwsIllegalArgumentException() {
        when(integrationClient.paymentStatus(any(), eq("pay-none"))).thenThrow(FeignException.NotFound.class);
        assertThatThrownBy(() -> walletService.confirmTopupPayment("pay-none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void confirmTopupPayment_whenFeignError_throwsIllegalStateException() {
        when(integrationClient.paymentStatus(any(), eq("pay-error"))).thenThrow(FeignException.ServiceUnavailable.class);
        assertThatThrownBy(() -> walletService.confirmTopupPayment("pay-error"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to fetch payment status");
    }

    @Test
    void confirmTopupPayment_whenStatusIsNull_throwsIllegalArgumentException() {
        when(integrationClient.paymentStatus(any(), eq("pay-null"))).thenReturn(null);
        assertThatThrownBy(() -> walletService.confirmTopupPayment("pay-null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void confirmTopupPayment_whenStatusNotSuccess_throwsIllegalStateException() {
        when(integrationClient.paymentStatus(any(), eq("pay-fail")))
                .thenReturn(new ExternalPaymentStatus("pay-fail", 1L, BigDecimal.TEN, "UPI", "FAILED"));
        assertThatThrownBy(() -> walletService.confirmTopupPayment("pay-fail"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment is not successful yet");
    }

    @Test
    void confirmTopupPayment_whenStatusSuccess_performsTopup() {
        mockDefaultLimitChecks();
        when(integrationClient.paymentStatus(any(), eq("pay-success")))
                .thenReturn(new ExternalPaymentStatus("pay-success", 1L, BigDecimal.TEN, "UPI", "SUCCESS"));
        
        WalletAccount account = new WalletAccount();
        account.setUserId(1L);
        account.setBalance(BigDecimal.ZERO);
        when(walletAccountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(walletAccountRepository.save(any())).thenReturn(account);

        PaymentTopupConfirmResponse response = walletService.confirmTopupPayment("pay-success");

        assertThat(response.paymentStatus()).isEqualTo("CAPTURED");
        verify(walletAccountRepository).save(any());
        verify(integrationClient).updatePaymentStatus(any(), eq("pay-success"), any());
    }

    @Test
    void transfer_whenReceiverNotFound_throwsIllegalArgumentException() {
        mockDefaultLimitChecks();
        when(userClient.getUserById(99L)).thenThrow(FeignException.NotFound.class);
        TransferRequest request = new TransferRequest(1L, 99L, BigDecimal.TEN);
        
        assertThatThrownBy(() -> walletService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Receiver user not found");
    }

    @Test
    void transfer_whenUserClientFails_throwsIllegalStateException() {
        mockDefaultLimitChecks();
        when(userClient.getUserById(2L)).thenThrow(FeignException.InternalServerError.class);
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.TEN);
        
        assertThatThrownBy(() -> walletService.transfer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to validate receiver user");
    }

    @Test
    void transfer_whenInsufficientBalance_throwsIllegalArgumentException() {
        mockDefaultLimitChecks();
        when(userClient.getUserById(2L)).thenReturn(new Object());
        
        WalletAccount from = new WalletAccount();
        from.setUserId(1L);
        from.setBalance(new BigDecimal("5.00"));
        when(walletAccountRepository.findByUserId(1L)).thenReturn(Optional.of(from));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("10.00"));
        assertThatThrownBy(() -> walletService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    void updateLimits_whenInvalidValues_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> walletService.updateLimits(new WalletLimitUpdateRequest(BigDecimal.ZERO, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.updateLimits(new WalletLimitUpdateRequest(null, BigDecimal.ZERO, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> walletService.updateLimits(new WalletLimitUpdateRequest(null, null, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

        void updateLimits_updatesAllValidFields() {
        WalletLimitConfig config = new WalletLimitConfig();
        config.setId(1L);
        when(walletLimitConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(walletLimitConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WalletLimitUpdateRequest request = new WalletLimitUpdateRequest(
                new BigDecimal("1000"), new BigDecimal("500"), 5);
        WalletLimitConfig result = walletService.updateLimits(request);

        assertThat(result.getDailyTopupLimit()).isEqualByComparingTo("1000");
        assertThat(result.getDailyTransferLimit()).isEqualByComparingTo("500");
        assertThat(result.getDailyTransferCountLimit()).isEqualTo(5);
    }

    @Test
    void confirmTopupPayment_whenStatusCaptured_returnsResponse() {
        ExternalPaymentStatus status = new ExternalPaymentStatus("pay-1", 3L, BigDecimal.TEN, "UPI", "CAPTURED");
        when(integrationClient.paymentStatus(any(), any())).thenReturn(status);
        when(walletAccountRepository.findByUserId(3L)).thenReturn(Optional.of(new WalletAccount()));

        PaymentTopupConfirmResponse resp = walletService.confirmTopupPayment("pay-1");
        assertThat(resp.paymentStatus()).isEqualTo("CAPTURED");
    }

    @Test
    void transfer_whenSameUser_throwsException() {
        TransferRequest req = new TransferRequest(1L, 1L, BigDecimal.TEN);
        assertThatThrownBy(() -> walletService.transfer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    void transfer_whenLimitExceeded_throwsException() {
        when(walletLimitConfigRepository.findById(any())).thenReturn(Optional.of(new WalletLimitConfig(1L, BigDecimal.TEN, BigDecimal.TEN, 1)));
        // Mock that user already did 1 transfer today
        when(ledgerEntryRepository.countByUserIdAndTypeAndCreatedAtBetween(any(), any(), any(), any())).thenReturn(1L);

        TransferRequest req = new TransferRequest(1L, 2L, BigDecimal.ONE);
        assertThatThrownBy(() -> walletService.transfer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count limit exceeded");
    }

    @Test
    void updateLimits_whenInvalidValues_throwsException() {
        when(walletLimitConfigRepository.findById(any())).thenReturn(Optional.of(new WalletLimitConfig()));
        
        WalletLimitUpdateRequest req = new WalletLimitUpdateRequest(BigDecimal.ZERO, null, null);
        assertThatThrownBy(() -> walletService.updateLimits(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void getBalance_whenUserIdNull_throwsException() {
        assertThatThrownBy(() -> walletService.getBalance(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void history_whenUserIdNull_throwsException() {
        assertThatThrownBy(() -> walletService.history(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topup_whenAmountZero_throwsException() {
        TopupRequest req = new TopupRequest(1L, BigDecimal.ZERO, "UPI");
        assertThatThrownBy(() -> walletService.topup(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOrCreateLimitConfig_whenNotFound_createsDefault() {
        when(walletLimitConfigRepository.findById(any())).thenReturn(Optional.empty());
        when(walletLimitConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WalletLimitConfig config = walletService.getLimits();
        assertThat(config.getDailyTopupLimit()).isEqualByComparingTo("50000");
    }
}
