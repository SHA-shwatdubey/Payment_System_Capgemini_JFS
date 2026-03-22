package com.wallet.wallet.repository;

import com.wallet.wallet.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.userId = :userId and l.type = :type and l.createdAt between :from and :to")
    BigDecimal sumByUserAndTypeAndCreatedAtBetween(@Param("userId") Long userId,
                                                    @Param("type") String type,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    long countByUserIdAndTypeAndCreatedAtBetween(Long userId, String type, LocalDateTime from, LocalDateTime to);
}


