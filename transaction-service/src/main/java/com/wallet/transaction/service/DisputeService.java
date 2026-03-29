package com.wallet.transaction.service;

import com.wallet.transaction.dto.DisputeCreateRequest;
import com.wallet.transaction.dto.DisputeResolveRequest;
import com.wallet.transaction.entity.Dispute;
import com.wallet.transaction.repository.DisputeRepository;
import com.wallet.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;

    public DisputeService(DisputeRepository disputeRepository,
                          TransactionRepository transactionRepository) {
        this.disputeRepository = disputeRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Dispute create(DisputeCreateRequest request) {
        if (!transactionRepository.existsById(request.transactionId())) {
            throw new IllegalArgumentException("Transaction not found");
        }

        Dispute dispute = new Dispute();
        dispute.setTransactionId(request.transactionId());
        dispute.setUserId(request.userId());
        dispute.setReason(request.reason());
        dispute.setStatus("OPEN");
        dispute.setEscalated(false);
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public List<Dispute> byUser(Long userId) {
        return disputeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Dispute> openDisputes() {
        return disputeRepository.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    @Transactional
    public Dispute escalate(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        dispute.setEscalated(true);
        dispute.setStatus("ESCALATED");
        return disputeRepository.save(dispute);
    }

    @Transactional
    public Dispute resolve(Long disputeId, DisputeResolveRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        dispute.setStatus("RESOLVED");
        dispute.setResolution(request.resolution());
        return disputeRepository.save(dispute);
    }
}

