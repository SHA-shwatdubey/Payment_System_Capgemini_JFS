package com.wallet.transaction.repository;

import com.wallet.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(Long userId, Long senderId, Long receiverId);

    List<TransactionReadProjection> findProjectedByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(Long userId, Long senderId, Long receiverId);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}


