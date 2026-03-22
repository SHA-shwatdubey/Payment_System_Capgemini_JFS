package com.wallet.wallet.repository;

import com.wallet.wallet.entity.WalletLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLimitConfigRepository extends JpaRepository<WalletLimitConfig, Long> {
}

