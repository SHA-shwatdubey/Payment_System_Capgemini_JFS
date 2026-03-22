package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.entity.EntryType;
import com.wallet.transaction.entity.LedgerEntry;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.exception.IdempotencyConflictException;
import com.wallet.transaction.exception.InsufficientBalanceException;
import com.wallet.transaction.exception.ResourceNotFoundException;
import com.wallet.transaction.repository.LedgerEntryRepository;
import com.wallet.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void topup_whenIdempotencyKeyExists_returnsExistingTransaction() {
        Transaction existing = new Transaction();
        existing.setId(11L);
        existing.setType(TransactionType.TOPUP);
        existing.setReceiverId(10L);
        existing.setAmount(new BigDecimal("100"));
        existing.setStatus(TransactionStatus.SUCCESS);

        when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(existing));

        TransactionResponse response = transactionService.topup(new TopupRequest(10L, new BigDecimal("100"), "k1"));

        assertThat(response.id()).isEqualTo(11L);
    }

    @Test
    void transfer_withInsufficientBalance_throwsExceptionAndMarksFailed() {
        List<TransactionStatus> savedStatuses = new ArrayList<>();
        List<String> savedIdempotencyKeys = new ArrayList<>();

        when(transactionRepository.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            savedStatuses.add(transaction.getStatus());
            savedIdempotencyKeys.add(transaction.getIdempotencyKey());
            return transaction;
        });
        when(userClient.getUserById(1L)).thenReturn(new Object());
        when(userClient.getUserById(2L)).thenReturn(new Object());
        when(ledgerEntryRepository.calculateBalanceForUpdate(1L)).thenReturn(new BigDecimal("5"));

        assertThatThrownBy(() -> transactionService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10"), "k2")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        assertThat(savedStatuses).containsExactly(TransactionStatus.PENDING, TransactionStatus.FAILED);
        assertThat(savedIdempotencyKeys).containsExactly("k2", "k2");

        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(rewardEventPublisher, never()).publish(any());
    }

    @Test
    void transfer_withSameUsers_throwsValidationError() {
        assertThatThrownBy(() -> transactionService.transfer(new TransferRequest(2L, 2L, new BigDecimal("1"), "k3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be same");
    }

    @Test
    void payment_withSameIdempotencyKeyDifferentPayload_throwsConflict() {
        Transaction existing = new Transaction();
        existing.setType(TransactionType.PAYMENT);
        existing.setSenderId(1L);
        existing.setReceiverId(2L);
        existing.setAmount(new BigDecimal("10"));
        when(transactionRepository.findByIdempotencyKey("p1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.payment(new PaymentRequest(1L, 2L, new BigDecimal("20"), "p1")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void refund_whenOriginalTransactionMissing_throwsNotFound() {
        when(transactionRepository.findByIdempotencyKey("r1")).thenReturn(Optional.empty());
        when(transactionRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.refund(new RefundRequest(1L, 2L, new BigDecimal("10"), 77L, "r1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Original transaction not found");
    }

    @Test
    void buildStatementCsv_whenRangeExceeds90Days_throwsValidationError() {
        LocalDateTime from = LocalDateTime.now().minusDays(120);
        LocalDateTime to = LocalDateTime.now();

        assertThatThrownBy(() -> transactionService.buildStatementCsv(1L, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 90 days");
    }

    @Test
    void buildStatementCsv_withEntries_includesRunningBalance() {
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);

        when(userClient.getUserById(1L)).thenReturn(new Object());
        when(ledgerEntryRepository.calculateBalanceBefore(1L, from)).thenReturn(new BigDecimal("100"));

        LedgerEntry credit = new LedgerEntry();
        credit.setCreatedAt(from.plusHours(1));
        credit.setEntryType(EntryType.CREDIT);
        credit.setAmount(new BigDecimal("30"));

        LedgerEntry debit = new LedgerEntry();
        debit.setCreatedAt(from.plusHours(2));
        debit.setEntryType(EntryType.DEBIT);
        debit.setAmount(new BigDecimal("10"));

        when(ledgerEntryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(1L, from, to))
                .thenReturn(List.of(credit, debit));

        String csv = new String(transactionService.buildStatementCsv(1L, from, to));

        assertThat(csv).contains("createdAt,entryType,amount,runningBalance");
        assertThat(csv).contains("CREDIT,30,130");
        assertThat(csv).contains("DEBIT,10,120");
    }
}




