package com.wallet.wallet.query.handler;

import com.wallet.wallet.query.dto.WalletBalanceView;
import com.wallet.wallet.query.dto.WalletHistoryItemView;
import com.wallet.wallet.repository.LedgerEntryRepository;
import com.wallet.wallet.repository.WalletAccountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletQueryHandler {

    private final WalletAccountRepository walletAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletQueryHandler(WalletAccountRepository walletAccountRepository,
                              LedgerEntryRepository ledgerEntryRepository) {
        this.walletAccountRepository = walletAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "walletBalance", key = "#userId")
    public WalletBalanceView getWallet(Long userId) {
        BigDecimal balance = walletAccountRepository.findByUserId(userId)
                .map(account -> account.getBalance())
                .orElse(BigDecimal.ZERO);
        return new WalletBalanceView(userId, balance);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "walletHistory", key = "#userId")
    public List<WalletHistoryItemView> getWalletHistory(Long userId) {
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WalletHistoryItemView::fromEntity)
                .toList();
    }
}

