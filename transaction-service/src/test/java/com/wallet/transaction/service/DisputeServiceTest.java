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

    @Mock private DisputeRepository disputeRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private DisputeService disputeService;

    @Test void create_validRequest_savesDispute() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(new Transaction()));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Dispute result = disputeService.create(new DisputeCreateRequest(1L, 10L, "Wrong charge"));
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getReason()).isEqualTo("Wrong charge");
        assertThat(result.isEscalated()).isFalse();
    }

    @Test void create_transactionNotFound_throws() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        DisputeCreateRequest req = new DisputeCreateRequest(99L, 10L, "reason");
        assertThatThrownBy(() -> disputeService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test void byUser_returnsList() {
        when(disputeRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(new Dispute()));
        assertThat(disputeService.byUser(1L)).hasSize(1);
    }

    @Test void openDisputes_returnsList() {
        when(disputeRepository.findByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(List.of(new Dispute()));
        assertThat(disputeService.openDisputes()).hasSize(1);
    }

    @Test void escalate_setsEscalatedAndStatus() {
        Dispute dispute = new Dispute(); dispute.setStatus("OPEN"); dispute.setEscalated(false);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Dispute result = disputeService.escalate(1L);
        assertThat(result.isEscalated()).isTrue();
        assertThat(result.getStatus()).isEqualTo("ESCALATED");
    }

    @Test void escalate_notFound_throws() {
        when(disputeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> disputeService.escalate(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void resolve_setsResolution() {
        Dispute dispute = new Dispute(); dispute.setStatus("OPEN");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Dispute result = disputeService.resolve(1L, new DisputeResolveRequest("Refunded"));
        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolution()).isEqualTo("Refunded");
    }

    @Test void resolve_notFound_throws() {
        when(disputeRepository.findById(99L)).thenReturn(Optional.empty());
        DisputeResolveRequest req = new DisputeResolveRequest("x");
        assertThatThrownBy(() -> disputeService.resolve(99L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
