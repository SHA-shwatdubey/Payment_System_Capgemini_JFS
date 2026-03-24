package com.wallet.wallet.service;

import com.wallet.wallet.client.IntegrationClient;
import com.wallet.wallet.client.UserClient;
import com.wallet.wallet.dto.ExternalPaymentStatus;
import com.wallet.wallet.dto.ExternalPaymentStatusUpdateRequest;
import com.wallet.wallet.dto.PaymentTopupConfirmResponse;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.WalletEvent;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.entity.LedgerEntry;
import com.wallet.wallet.entity.WalletAccount;
import com.wallet.wallet.entity.WalletLimitConfig;
import com.wallet.wallet.repository.LedgerEntryRepository;
import com.wallet.wallet.repository.WalletAccountRepository;
import com.wallet.wallet.repository.WalletLimitConfigRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private static final Long LIMIT_CONFIG_ID = 1L;
    private static final String TYPE_TOPUP = "TOPUP";
    private static final String TYPE_TRANSFER_DEBIT = "TRANSFER_DEBIT";
    private static final String TYPE_TRANSFER_CREDIT = "TRANSFER_CREDIT";

    private final WalletAccountRepository walletAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletEventPublisher eventPublisher;
    private final UserClient userClient;
    private final WalletLimitConfigRepository walletLimitConfigRepository;
    private final NotificationClient notificationClient;
    private final IntegrationClient integrationClient;

    public WalletService(WalletAccountRepository walletAccountRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         @Qualifier("walletDomainEventPublisher") WalletEventPublisher eventPublisher,
                         UserClient userClient,
                         WalletLimitConfigRepository walletLimitConfigRepository,
                         NotificationClient notificationClient,
                         IntegrationClient integrationClient) {
        this.walletAccountRepository = walletAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.eventPublisher = eventPublisher;
        this.userClient = userClient;
        this.walletLimitConfigRepository = walletLimitConfigRepository;
        this.notificationClient = notificationClient;
        this.integrationClient = integrationClient;
    }

    public BigDecimal getBalance(Long userId) {
        return getOrCreateAccount(userId).getBalance();
    }

    @Transactional
    @CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)
    public WalletAccount topup(TopupRequest request) {
        validateTopupLimit(request.userId(), request.amount());

        WalletAccount account = getOrCreateAccount(request.userId());
        account.setBalance(account.getBalance().add(request.amount()));
        walletAccountRepository.save(account);

        saveLedger(request.userId(), TYPE_TOPUP, request.amount());
        eventPublisher.publish(new WalletEvent(request.userId(), TYPE_TOPUP, request.amount()));

        notificationClient.sendSafe(
                request.userId(),
                "WALLET_CREDIT",
                "SMS",
                "user-" + request.userId(),
                "Wallet credited with amount " + request.amount()
        );
        notificationClient.sendSafe(
                request.userId(),
                "WALLET_CREDIT",
                "EMAIL",
                "user-" + request.userId(),
                "Wallet credited successfully. Amount: " + request.amount()
        );

        return account;
    }

    public PaymentTopupInitResponse initTopupPayment(PaymentTopupInitRequest request) {
        return integrationClient.initPayment(IntegrationClient.INTERNAL_CALL_VALUE, request);
    }

    @Transactional
    public PaymentTopupConfirmResponse confirmTopupPayment(String paymentRef) {
        ExternalPaymentStatus status;
        try {
            status = integrationClient.paymentStatus(IntegrationClient.INTERNAL_CALL_VALUE, paymentRef);
        } catch (FeignException.NotFound ex) {
            throw new IllegalArgumentException("Payment not found for reference: " + paymentRef);
        } catch (FeignException ex) {
            throw new IllegalStateException("Unable to fetch payment status from integration service");
        }

        if (status == null) {
            throw new IllegalArgumentException("Payment not found for reference: " + paymentRef);
        }
        if ("CAPTURED".equalsIgnoreCase(status.status())) {
            WalletAccount account = getOrCreateAccount(status.userId());
            return new PaymentTopupConfirmResponse(paymentRef, "CAPTURED", account);
        }
        if (!"SUCCESS".equalsIgnoreCase(status.status())) {
            throw new IllegalStateException("Payment is not successful yet. Current status: " + status.status());
        }

        WalletAccount account = topup(new TopupRequest(status.userId(), status.amount(), status.method()));
        try {
            integrationClient.updatePaymentStatus(
                    IntegrationClient.INTERNAL_CALL_VALUE,
                    paymentRef,
                    new ExternalPaymentStatusUpdateRequest("CAPTURED", "Wallet credited")
            );
        } catch (FeignException ex) {
            throw new IllegalStateException("Wallet credited, but failed to mark payment as CAPTURED in integration service");
        }
        return new PaymentTopupConfirmResponse(paymentRef, "CAPTURED", account);
    }

    @Transactional
    @CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)
    public String transfer(TransferRequest request) {
        validateTransferRequest(request);
        validateTransferLimits(request.fromUserId(), request.amount());
        try {
            userClient.getUserById(request.toUserId());
        } catch (FeignException.NotFound ex) {
            throw new IllegalArgumentException("Receiver user not found: " + request.toUserId());
        } catch (FeignException ex) {
            throw new IllegalStateException("Unable to validate receiver user right now. Please try again.");
        }

        WalletAccount fromAccount = getOrCreateAccount(request.fromUserId());
        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        WalletAccount toAccount = getOrCreateAccount(request.toUserId());
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        walletAccountRepository.save(fromAccount);
        walletAccountRepository.save(toAccount);

        saveLedger(request.fromUserId(), TYPE_TRANSFER_DEBIT, request.amount().negate());
        saveLedger(request.toUserId(), TYPE_TRANSFER_CREDIT, request.amount());

        eventPublisher.publish(new WalletEvent(request.fromUserId(), TYPE_TRANSFER_DEBIT, request.amount()));

        notificationClient.sendSafe(
                request.fromUserId(),
                "TRANSFER_SENT",
                "SMS",
                "user-" + request.fromUserId(),
                "Transfer successful. Amount sent: " + request.amount()
        );
        notificationClient.sendSafe(
                request.toUserId(),
                "TRANSFER_RECEIVED",
                "SMS",
                "user-" + request.toUserId(),
                "Amount received: " + request.amount()
        );

        return "Transfer successful";
    }

    public List<LedgerEntry> history(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public WalletLimitConfig getLimits() {
        return getOrCreateLimitConfig();
    }

    @Transactional
    @CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)
    public WalletLimitConfig updateLimits(WalletLimitUpdateRequest request) {
        WalletLimitConfig config = getOrCreateLimitConfig();

        if (request.dailyTopupLimit() != null) {
            if (request.dailyTopupLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Daily topup limit must be greater than zero");
            }
            config.setDailyTopupLimit(request.dailyTopupLimit());
        }

        if (request.dailyTransferLimit() != null) {
            if (request.dailyTransferLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Daily transfer limit must be greater than zero");
            }
            config.setDailyTransferLimit(request.dailyTransferLimit());
        }

        if (request.dailyTransferCountLimit() != null) {
            if (request.dailyTransferCountLimit() <= 0) {
                throw new IllegalArgumentException("Daily transfer count limit must be greater than zero");
            }
            config.setDailyTransferCountLimit(request.dailyTransferCountLimit());
        }

        return walletLimitConfigRepository.save(config);
    }

    private WalletAccount getOrCreateAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return walletAccountRepository.findByUserId(userId).orElseGet(() -> {
            WalletAccount account = new WalletAccount();
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO);
            return walletAccountRepository.save(account);
        });
    }

    private void saveLedger(Long userId, String type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setUserId(userId);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setReference(UUID.randomUUID().toString());
        entry.setCreatedAt(LocalDateTime.now());
        ledgerEntryRepository.save(entry);
    }

    private WalletLimitConfig getOrCreateLimitConfig() {
        return walletLimitConfigRepository.findById(LIMIT_CONFIG_ID).orElseGet(() -> {
            WalletLimitConfig config = new WalletLimitConfig();
            config.setId(LIMIT_CONFIG_ID);
            config.setDailyTopupLimit(new BigDecimal("50000"));
            config.setDailyTransferLimit(new BigDecimal("25000"));
            config.setDailyTransferCountLimit(10);
            return walletLimitConfigRepository.save(config);
        });
    }

    private void validateTopupLimit(Long userId, BigDecimal amount) {
        validatePositiveAmount(amount, "Topup amount must be greater than zero");
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }

        WalletLimitConfig limitConfig = getOrCreateLimitConfig();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        BigDecimal dailyTopup = ledgerEntryRepository
                .sumByUserAndTypeAndCreatedAtBetween(userId, TYPE_TOPUP, start, end);
        BigDecimal newTotal = dailyTopup.add(amount);
        if (newTotal.compareTo(limitConfig.getDailyTopupLimit()) > 0) {
            throw new IllegalArgumentException("Daily topup limit exceeded");
        }
    }

    private void validateTransferLimits(Long fromUserId, BigDecimal amount) {
        validatePositiveAmount(amount, "Transfer amount must be greater than zero");
        if (fromUserId == null) {
            throw new IllegalArgumentException("Sender user id is required");
        }

        WalletLimitConfig limitConfig = getOrCreateLimitConfig();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        BigDecimal dailyTransferDebit = ledgerEntryRepository
                .sumByUserAndTypeAndCreatedAtBetween(fromUserId, TYPE_TRANSFER_DEBIT, start, end)
                .abs();
        BigDecimal transferTotalAfter = dailyTransferDebit.add(amount);
        if (transferTotalAfter.compareTo(limitConfig.getDailyTransferLimit()) > 0) {
            throw new IllegalArgumentException("Daily transfer limit exceeded");
        }

        long transferCount = ledgerEntryRepository
                .countByUserIdAndTypeAndCreatedAtBetween(fromUserId, TYPE_TRANSFER_DEBIT, start, end);
        if (transferCount >= limitConfig.getDailyTransferCountLimit()) {
            throw new IllegalArgumentException("Daily transfer count limit exceeded");
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request is required");
        }
        if (request.fromUserId() == null || request.toUserId() == null) {
            throw new IllegalArgumentException("Both sender and receiver user ids are required");
        }
        if (request.fromUserId().equals(request.toUserId())) {
            throw new IllegalArgumentException("Sender and receiver cannot be the same");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}

