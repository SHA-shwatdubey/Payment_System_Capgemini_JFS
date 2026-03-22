package com.wallet.transaction.service;

import com.wallet.transaction.dto.DisputeCreateRequest;
import com.wallet.transaction.dto.DisputeResolveRequest;
import com.wallet.transaction.entity.Dispute;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.repository.DisputeRepository;
import com.wallet.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DisputeService disputeService;

    @Test
    void create_withKnownTransaction_savesOpenDispute() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(new Transaction()));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dispute result = disputeService.create(new DisputeCreateRequest(10L, 7L, "duplicate debit"));

        assertThat(result.getTransactionId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.isEscalated()).isFalse();
    }

    @Test
    void create_withUnknownTransaction_throwsValidationError() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disputeService.create(new DisputeCreateRequest(999L, 5L, "bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void resolve_updatesStatusAndResolution() {
        Dispute dispute = new Dispute();
        dispute.setId(22L);
        dispute.setStatus("OPEN");
        when(disputeRepository.findById(22L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dispute result = disputeService.resolve(22L, new DisputeResolveRequest("refunded"));

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolution()).isEqualTo("refunded");
        verify(disputeRepository).save(dispute);
    }

    @Test
    void byUser_delegatesToRepository() {
        when(disputeRepository.findByUserIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(new Dispute()));

        assertThat(disputeService.byUser(3L)).hasSize(1);
    }
}

