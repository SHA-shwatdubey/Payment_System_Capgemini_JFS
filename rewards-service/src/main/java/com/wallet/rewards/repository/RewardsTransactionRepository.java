package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardsTransactionRepository extends JpaRepository<RewardsTransaction, Long> {
}

