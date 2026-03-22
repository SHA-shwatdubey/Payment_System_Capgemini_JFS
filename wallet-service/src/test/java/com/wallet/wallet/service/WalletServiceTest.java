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
        when(walletAccountRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
                .thenReturn(new ExternalPaymentStatus("pay-2", 5L, new BigDecimal("40"), "UPI", "CAPTURED"));
        when(walletAccountRepository.findByUserId(5L)).thenReturn(Optional.of(account));

        PaymentTopupConfirmResponse response = walletService.confirmTopupPayment("pay-2");

        assertThat(response.paymentStatus()).isEqualTo("CAPTURED");
        assertThat(response.walletAccount().getBalance()).isEqualByComparingTo("200");
    }

    @Test
    void transfer_withInsufficientBalance_throwsValidationError() {
        mockDefaultLimitChecks();
        when(userClient.getUserById(9L)).thenReturn(new Object());

        WalletAccount from = new WalletAccount();
        from.setUserId(1L);
        from.setBalance(new BigDecimal("2"));
        when(walletAccountRepository.findByUserId(1L)).thenReturn(Optional.of(from));

        assertThatThrownBy(() -> walletService.transfer(new TransferRequest(1L, 9L, new BigDecimal("10"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    void updateLimits_withInvalidTopupLimit_throwsValidationError() {
        WalletLimitConfig config = new WalletLimitConfig();
        config.setId(1L);
        config.setDailyTopupLimit(new BigDecimal("100"));
        config.setDailyTransferLimit(new BigDecimal("50"));
        config.setDailyTransferCountLimit(2);
        when(walletLimitConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> walletService.updateLimits(new WalletLimitUpdateRequest(BigDecimal.ZERO, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily topup limit must be greater than zero");
    }
}







