package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransactionHistoryEvent;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.dto.WalletEvent;
import com.wallet.transaction.entity.EntryType;
import com.wallet.transaction.entity.LedgerEntry;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.exception.IdempotencyConflictException;
import com.wallet.transaction.exception.InsufficientBalanceException;
import com.wallet.transaction.exception.ResourceNotFoundException;
import com.wallet.transaction.repository.LedgerEntryRepository;
import com.wallet.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final Long SYSTEM_ACCOUNT_ID = 0L;

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserClient userClient;
    private final RewardEventPublisher rewardEventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              UserClient userClient,
                              RewardEventPublisher rewardEventPublisher,
                              TransactionEventPublisher transactionEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userClient = userClient;
        this.rewardEventPublisher = rewardEventPublisher;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public TransactionResponse topup(TopupRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            validateExistingForTopup(existing.get(), request);
            return TransactionResponse.from(existing.get());
        }

        Transaction transaction = createPendingTransaction(
                request.userId(),
                SYSTEM_ACCOUNT_ID,
                request.userId(),
                request.amount(),
                TransactionType.TOPUP,
                request.idempotencyKey()
        );

        try {
            validateUser(request.userId());
            saveLedgerPair(transaction, SYSTEM_ACCOUNT_ID, request.userId(), request.amount());
            markSuccess(transaction);
            publishRewardEvent(request.userId(), transaction.getType(), request.amount());
            publishTransactionHistoryEvent(transaction);
            log.info("Topup successful transactionId={} userId={} amount={}", transaction.getId(), request.userId(), request.amount());
            return TransactionResponse.from(transaction);
        } catch (RuntimeException ex) {
            markFailed(transaction);
            throw ex;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public TransactionResponse transfer(TransferRequest request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new IllegalArgumentException("Sender and receiver cannot be same for transfer");
        }

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            validateExistingForTransfer(existing.get(), request);
            return TransactionResponse.from(existing.get());
        }

        Transaction transaction = createPendingTransaction(
                request.senderId(),
                request.senderId(),
                request.receiverId(),
                request.amount(),
                TransactionType.TRANSFER,
                request.idempotencyKey()
        );

        try {
            validateUser(request.senderId());
            validateUser(request.receiverId());
            ensureSufficientBalance(request.senderId(), request.amount());
            saveLedgerPair(transaction, request.senderId(), request.receiverId(), request.amount());
            markSuccess(transaction);
            publishRewardEvent(request.senderId(), transaction.getType(), request.amount());
            publishTransactionHistoryEvent(transaction);
            log.info("Transfer successful transactionId={} senderId={} receiverId={} amount={}",
                    transaction.getId(), request.senderId(), request.receiverId(), request.amount());
            return TransactionResponse.from(transaction);
        } catch (RuntimeException ex) {
            markFailed(transaction);
            throw ex;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public TransactionResponse payment(PaymentRequest request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new IllegalArgumentException("Sender and receiver cannot be same for payment");
        }

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            validateExistingForPayment(existing.get(), request);
            return TransactionResponse.from(existing.get());
        }

        Transaction transaction = createPendingTransaction(
                request.senderId(),
                request.senderId(),
                request.receiverId(),
                request.amount(),
                TransactionType.PAYMENT,
                request.idempotencyKey()
        );

        try {
            validateUser(request.senderId());
            validateUser(request.receiverId());
            ensureSufficientBalance(request.senderId(), request.amount());
            saveLedgerPair(transaction, request.senderId(), request.receiverId(), request.amount());
            markSuccess(transaction);
            publishRewardEvent(request.senderId(), transaction.getType(), request.amount());
            publishTransactionHistoryEvent(transaction);
            log.info("Payment successful transactionId={} senderId={} receiverId={} amount={}",
                    transaction.getId(), request.senderId(), request.receiverId(), request.amount());
            return TransactionResponse.from(transaction);
        } catch (RuntimeException ex) {
            markFailed(transaction);
            throw ex;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public TransactionResponse refund(RefundRequest request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new IllegalArgumentException("Sender and receiver cannot be same for refund");
        }

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            validateExistingForRefund(existing.get(), request);
            return TransactionResponse.from(existing.get());
        }

        Transaction original = transactionRepository.findById(request.originalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Original transaction not found"));

        if (original.getType() != TransactionType.PAYMENT && original.getType() != TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Refund can only reference PAYMENT or TRANSFER transaction");
        }

        Transaction transaction = createPendingTransaction(
                request.senderId(),
                request.senderId(),
                request.receiverId(),
                request.amount(),
                TransactionType.REFUND,
                request.idempotencyKey()
        );

        try {
            validateUser(request.senderId());
            validateUser(request.receiverId());
            ensureSufficientBalance(request.senderId(), request.amount());
            saveLedgerPair(transaction, request.senderId(), request.receiverId(), request.amount());
            markSuccess(transaction);
            publishRewardEvent(request.receiverId(), transaction.getType(), request.amount());
            publishTransactionHistoryEvent(transaction);
            log.info("Refund successful transactionId={} senderId={} receiverId={} amount={} originalTransactionId={}",
                    transaction.getId(), request.senderId(), request.receiverId(), request.amount(), request.originalTransactionId());
            return TransactionResponse.from(transaction);
        } catch (RuntimeException ex) {
            markFailed(transaction);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "transactionHistory", key = "#userId")
    public List<TransactionResponse> getByUser(Long userId) {
        validateUser(userId);
        return transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, userId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "transactionHistory", key = "#from.toString() + '-' + #to.toString()")
    public List<TransactionResponse> getHistory(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return transactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private Transaction createPendingTransaction(Long userId,
                                                 Long senderId,
                                                 Long receiverId,
                                                 BigDecimal amount,
                                                 TransactionType type,
                                                 String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(transaction);
    }

    private void validateUser(Long userId) {
        Object user = userClient.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
    }

    private void ensureSufficientBalance(Long userId, BigDecimal amount) {
        BigDecimal balance = ledgerEntryRepository.calculateBalanceForUpdate(userId);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for userId=" + userId);
        }
    }

    private void saveLedgerPair(Transaction transaction, Long senderId, Long receiverId, BigDecimal amount) {
        LedgerEntry debit = new LedgerEntry();
        debit.setTransaction(transaction);
        debit.setUserId(senderId);
        debit.setEntryType(EntryType.DEBIT);
        debit.setAmount(amount);

        LedgerEntry credit = new LedgerEntry();
        credit.setTransaction(transaction);
        credit.setUserId(receiverId);
        credit.setEntryType(EntryType.CREDIT);
        credit.setAmount(amount);

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);
    }

    private void markSuccess(Transaction transaction) {
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    private void markFailed(Transaction transaction) {
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    private void publishRewardEvent(Long userId, TransactionType type, BigDecimal amount) {
        rewardEventPublisher.publish(new WalletEvent(userId, type.name(), amount));
    }

    private void publishTransactionHistoryEvent(Transaction transaction) {
        try {
            TransactionHistoryEvent event = new TransactionHistoryEvent(
                    transaction.getId(),
                    transaction.getUserId(),
                    transaction.getSenderId(),
                    transaction.getReceiverId(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getCreatedAt(),
                    transaction.getIdempotencyKey()
            );
            transactionEventPublisher.publishTransactionEvent(event);
            log.debug("Published transaction history event for transactionId={}", transaction.getId());
        } catch (Exception e) {
            log.error("Error publishing transaction history event for transactionId={}", transaction.getId(), e);
            // Don't fail the transaction if event publishing fails
        }
    }

    private void validateExistingForTopup(Transaction existing, TopupRequest request) {
        if (existing.getType() != TransactionType.TOPUP
                || !request.userId().equals(existing.getReceiverId())
                || existing.getAmount().compareTo(request.amount()) != 0) {
            throw new IdempotencyConflictException("Idempotency key belongs to a different topup payload");
        }
    }

    private void validateExistingForTransfer(Transaction existing, TransferRequest request) {
        if (existing.getType() != TransactionType.TRANSFER
                || !request.senderId().equals(existing.getSenderId())
                || !request.receiverId().equals(existing.getReceiverId())
                || existing.getAmount().compareTo(request.amount()) != 0) {
            throw new IdempotencyConflictException("Idempotency key belongs to a different transfer payload");
        }
    }

    private void validateExistingForPayment(Transaction existing, PaymentRequest request) {
        if (existing.getType() != TransactionType.PAYMENT
                || !request.senderId().equals(existing.getSenderId())
                || !request.receiverId().equals(existing.getReceiverId())
                || existing.getAmount().compareTo(request.amount()) != 0) {
            throw new IdempotencyConflictException("Idempotency key belongs to a different payment payload");
        }
    }

    private void validateExistingForRefund(Transaction existing, RefundRequest request) {
        if (existing.getType() != TransactionType.REFUND
                || !request.senderId().equals(existing.getSenderId())
                || !request.receiverId().equals(existing.getReceiverId())
                || existing.getAmount().compareTo(request.amount()) != 0) {
            throw new IdempotencyConflictException("Idempotency key belongs to a different refund payload");
        }
    }

    @Transactional(readOnly = true)
    public byte[] buildReceiptPdf(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        String content = "Receipt\n"
                + "Transaction ID: " + transaction.getId() + "\n"
                + "User ID: " + transaction.getUserId() + "\n"
                + "Sender ID: " + transaction.getSenderId() + "\n"
                + "Receiver ID: " + transaction.getReceiverId() + "\n"
                + "Amount: " + transaction.getAmount() + "\n"
                + "Type: " + transaction.getType() + "\n"
                + "Status: " + transaction.getStatus() + "\n"
                + "Date: " + transaction.getCreatedAt() + "\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] buildStatementCsv(Long userId, LocalDateTime from, LocalDateTime to) {
        validateStatementWindow(from, to);
        validateUser(userId);

        BigDecimal running = ledgerEntryRepository.calculateBalanceBefore(userId, from);
        List<LedgerEntry> entries = ledgerEntryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("createdAt,entryType,amount,runningBalance\n");
        for (LedgerEntry entry : entries) {
            BigDecimal delta = entry.getEntryType() == EntryType.CREDIT
                    ? entry.getAmount()
                    : entry.getAmount().negate();
            running = running.add(delta);
            csv.append(entry.getCreatedAt()).append(",")
                    .append(entry.getEntryType()).append(",")
                    .append(entry.getAmount()).append(",")
                    .append(running)
                    .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] buildStatementPdf(Long userId, LocalDateTime from, LocalDateTime to) {
        validateStatementWindow(from, to);
        validateUser(userId);

        BigDecimal running = ledgerEntryRepository.calculateBalanceBefore(userId, from);
        List<LedgerEntry> entries = ledgerEntryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, from, to);

        StringBuilder content = new StringBuilder();
        content.append("Statement\n")
                .append("User ID: ").append(userId).append("\n")
                .append("From: ").append(from).append("\n")
                .append("To: ").append(to).append("\n\n");

        for (LedgerEntry entry : entries) {
            BigDecimal delta = entry.getEntryType() == EntryType.CREDIT
                    ? entry.getAmount()
                    : entry.getAmount().negate();
            running = running.add(delta);
            content.append(entry.getCreatedAt())
                    .append(" | ")
                    .append(entry.getEntryType())
                    .append(" | amount=")
                    .append(entry.getAmount())
                    .append(" | running=")
                    .append(running)
                    .append("\n");
        }

        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validateStatementWindow(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        if (Duration.between(from, to).toDays() > 90) {
            throw new IllegalArgumentException("Statement range cannot exceed 90 days");
        }
    }
}

