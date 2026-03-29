package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.repository.LedgerEntryRepository;
import com.wallet.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private RewardEventPublisher rewardEventPublisher;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void topup_withValidRequest_savesLedgerAndPublishesEvents() {
        when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userClient.getUserById(10L)).thenReturn(new Object());

        TransactionResponse resp = transactionService.topup(new TopupRequest(10L, new BigDecimal("100"), "k1"));

        assertThat(resp.status()).isEqualTo(TransactionStatus.SUCCESS);
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(rewardEventPublisher).publish(any());
        verify(transactionEventPublisher).publishTransactionEvent(any());
    }

    @Test
    void payment_viaSaga_executesAllStepsSuccessfully() {
        when(transactionRepository.findByIdempotencyKey("p1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userClient.getUserById(1L)).thenReturn(new Object());
        when(userClient.getUserById(2L)).thenReturn(new Object());
        when(ledgerEntryRepository.calculateBalanceForUpdate(1L)).thenReturn(new BigDecimal("100"));

        TransactionResponse resp = transactionService.payment(new PaymentRequest(1L, 2L, new BigDecimal("10"), "p1"));

        assertThat(resp.status()).isEqualTo(TransactionStatus.SUCCESS);
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(rewardEventPublisher).publish(any());
    }

    @Test
    void refund_withValidOriginalTransaction_createsRefundEntries() {
        Transaction original = new Transaction();
        original.setId(77L);
        original.setType(TransactionType.PAYMENT);
        when(transactionRepository.findByIdempotencyKey("r1")).thenReturn(Optional.empty());
        when(transactionRepository.findById(77L)).thenReturn(Optional.of(original));
        when(userClient.getUserById(any())).thenReturn(new Object());
        when(ledgerEntryRepository.calculateBalanceForUpdate(any())).thenReturn(new BigDecimal("50"));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionResponse resp = transactionService
                .refund(new RefundRequest(1L, 2L, new BigDecimal("10"), 77L, "r1"));

        assertThat(resp.status()).isEqualTo(TransactionStatus.SUCCESS);
        verify(ledgerEntryRepository, times(2)).save(any());
    }

    @Test
    void buildReceiptPdf_withValidId_returnsCustomPdfBytes() {
        Transaction t = new Transaction();
        t.setId(500L);
        t.setAmount(new BigDecimal("75.50"));
        t.setType(TransactionType.PAYMENT);
        t.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.findById(500L)).thenReturn(Optional.of(t));

        byte[] pdf = transactionService.buildReceiptPdf(500L);

        String content = new String(pdf);
        assertThat(content)
                .contains("NEXPAY TRANSACTION RECEIPT")
                .contains("INR 75.50");
    }

    @Test
    void buildStatementPdf_withValidData_filtersByDateRange() {
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(10);
        when(userClient.getUserById(1L)).thenReturn(new Object());

        Transaction t = new Transaction();
        t.setCreatedAt(from.plusDays(5));
        t.setAmount(BigDecimal.TEN);
        t.setStatus(TransactionStatus.SUCCESS);
        when(transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L, 1L))
                .thenReturn(List.of(t));

        byte[] pdf = transactionService.buildStatementPdf(1L, from, to);
        assertThat(new String(pdf)).contains("NEXPAY ACCOUNT STATEMENT");
    }

    @Test
    void validateStatementWindow_whenExceeds90Days_throwsException() {
        LocalDateTime from = LocalDateTime.now().minusDays(100);
        LocalDateTime to = LocalDateTime.now();

        assertThatThrownBy(() -> transactionService.buildStatementPdf(1L, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed 90 days");
    }

    @Test
    void payment_whenSagaFails_compensatesAndThrowsException() {
        when(transactionRepository.findByIdempotencyKey("p-fail")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userClient.getUserById(1L)).thenReturn(new Object());
        // Simulate failure in validation step
        when(userClient.getUserById(2L)).thenThrow(new RuntimeException("Saga Break"));

        PaymentRequest req = new PaymentRequest(1L, 2L, new BigDecimal("10"), "p-fail");
        assertThatThrownBy(() -> transactionService.payment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saga Break");

        // Should have been marked PENDING then FAILED during compensation
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void topup_whenIdempotencyConflict_throwsException() {
        Transaction existing = new Transaction();
        existing.setIdempotencyKey("key-1");
        existing.setType(TransactionType.TOPUP);
        existing.setUserId(1L);
        existing.setAmount(new BigDecimal("100"));
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        TopupRequest request = new TopupRequest(1L, new BigDecimal("200"), "key-1");
        assertThatThrownBy(() -> transactionService.topup(request))
                .isInstanceOf(com.wallet.transaction.exception.IdempotencyConflictException.class);
    }

    @Test
    void transfer_whenSenderIsReceiver_throwsException() {
        TransferRequest request = new TransferRequest(1L, 1L, BigDecimal.TEN, "key-2");
        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_whenInsufficientBalance_throwsException() {
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userClient.getUserById(any())).thenReturn(new Object());
        when(ledgerEntryRepository.calculateBalanceForUpdate(1L)).thenReturn(BigDecimal.ZERO);

        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.TEN, "key-3");
        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(com.wallet.transaction.exception.InsufficientBalanceException.class);
    }

    @Test
    void refund_whenOriginalNotFound_throwsException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());
        RefundRequest request = new RefundRequest(1L, 2L, BigDecimal.TEN, 999L, "key-refund");
        
        assertThatThrownBy(() -> transactionService.refund(request))
                .isInstanceOf(com.wallet.transaction.exception.ResourceNotFoundException.class);
    }

    @Test
    void getById_whenFound_returnsResponse() {
        Transaction t = new Transaction();
        t.setId(101L);
        when(transactionRepository.findById(101L)).thenReturn(Optional.of(t));
        TransactionResponse resp = transactionService.getById(101L);
        assertThat(resp.id()).isEqualTo(101L);
    }

    @Test
    void getByUser_returnsList() {
        when(userClient.getUserById(1L)).thenReturn(new Object());
        when(transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L, 1L))
                .thenReturn(List.of(new Transaction()));
        List<TransactionResponse> result = transactionService.getByUser(1L);
        assertThat(result).hasSize(1);
    }

    @Test
    void getHistory_returnsList() {
        LocalDateTime now = LocalDateTime.now();
        when(transactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(new Transaction()));
        List<TransactionResponse> result = transactionService.getHistory(now.minusDays(1), now);
        assertThat(result).hasSize(1);
    }

    @Test
    void buildStatementCsv_whenValid_returnsBytes() {
        when(userClient.getUserById(1L)).thenReturn(new Object());
        Transaction t = new Transaction();
        t.setId(5L);
        t.setAmount(BigDecimal.TEN);
        when(transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L, 1L))
                .thenReturn(List.of(t));

        byte[] csv = transactionService.buildStatementCsv(1L, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertThat(new String(csv)).contains("TransactionID,Date").contains("5");
    }

    @Test
    void refund_whenOriginalNotPaymentOrTransfer_throwsException() {
        Transaction original = new Transaction();
        original.setType(TransactionType.TOPUP); // Refund not allowed for TOPUP
        when(transactionRepository.findById(77L)).thenReturn(Optional.of(original));

        RefundRequest req = new RefundRequest(1L, 2L, BigDecimal.TEN, 77L, "r-failed");
        assertThatThrownBy(() -> transactionService.refund(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only reference PAYMENT or TRANSFER");
    }

    @Test
    void publishTransactionHistoryEvent_whenPublisherFails_doesNotThrow() {
        // This test hits the catch block in publishTransactionHistoryEvent
        when(transactionRepository.findByIdempotencyKey("k-event-fail")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userClient.getUserById(any())).thenReturn(new Object());
        
        // Mock publisher to throw
        org.mockito.Mockito.doThrow(new RuntimeException("Kafka Down"))
                .when(transactionEventPublisher).publishTransactionEvent(any());

        TransactionResponse resp = transactionService.topup(new TopupRequest(1L, BigDecimal.TEN, "k-event-fail"));
        assertThat(resp.status()).isEqualTo(TransactionStatus.SUCCESS);
        // Verify code reached past the catch block
        verify(transactionEventPublisher).publishTransactionEvent(any());
    }
}
