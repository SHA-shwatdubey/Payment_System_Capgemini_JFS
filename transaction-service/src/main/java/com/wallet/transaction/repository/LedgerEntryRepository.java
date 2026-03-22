package com.wallet.transaction.repository;

import com.wallet.transaction.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("select coalesce(sum(case when l.entryType = 'CREDIT' then l.amount else -l.amount end), 0) from LedgerEntry l where l.userId = :userId")
    BigDecimal calculateBalance(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coalesce(sum(case when l.entryType = 'CREDIT' then l.amount else -l.amount end), 0) from LedgerEntry l where l.userId = :userId")
    BigDecimal calculateBalanceForUpdate(@Param("userId") Long userId);

    @Query("select coalesce(sum(case when l.entryType = 'CREDIT' then l.amount else -l.amount end), 0) from LedgerEntry l where l.userId = :userId and l.createdAt < :from")
    BigDecimal calculateBalanceBefore(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    List<LedgerEntry> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long userId, LocalDateTime from, LocalDateTime to);
}


