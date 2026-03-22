package com.wallet.transaction.repository;

import com.wallet.transaction.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Dispute> findByStatusOrderByCreatedAtDesc(String status);
}

