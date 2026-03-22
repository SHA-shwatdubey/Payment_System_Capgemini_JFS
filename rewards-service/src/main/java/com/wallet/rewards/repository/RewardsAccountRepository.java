package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardsAccountRepository extends JpaRepository<RewardsAccount, Long> {
    Optional<RewardsAccount> findByUserId(Long userId);
}

