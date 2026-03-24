package com.wallet.transaction.query.handler;

import com.wallet.transaction.query.dto.TransactionReadDto;
import com.wallet.transaction.repository.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionQueryHandler {

    private final TransactionRepository transactionRepository;

    public TransactionQueryHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "transactionHistory", key = "#userId")
    public List<TransactionReadDto> getTransactionsByUserId(Long userId) {
        return transactionRepository.findProjectedByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, userId)
                .stream()
                .map(TransactionReadDto::fromProjection)
                .toList();
    }
}

