package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import com.wallet.transaction.dto.TransactionHistoryEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.wallet.transaction.saga.PaymentSagaContext;
import com.wallet.transaction.saga.SagaOrchestrator;
import com.wallet.transaction.saga.SagaStep;
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
                request.userId(),
                request.amount(),
                TransactionType.TOPUP,
                request.idempotencyKey()
        );

        try {
            validateUser(request.userId());
            saveLedgerPair(transaction, SYSTEM_ACCOUNT_ID, request.userId(), request.amount());
            markSuccess(transaction);
            try {
                publishRewardEvent(request.userId(), transaction.getType(), request.amount());
            } catch (Exception e) {
                log.error("Failed to publish reward event for topup: {}", e.getMessage());
            }
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
            try {
                publishRewardEvent(request.senderId(), transaction.getType(), request.amount());
            } catch (Exception e) {
                log.error("Failed to publish reward event for transfer: {}", e.getMessage());
            }
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

        // --- Saga Implementation Starts ---
        log.info("Initiating Payment Saga for payment from sender={} to receiver={} amount={}",
                request.senderId(), request.receiverId(), request.amount());

        PaymentSagaContext context = new PaymentSagaContext(request.senderId(), request.receiverId(), request.amount());
        SagaOrchestrator<PaymentSagaContext> orchestrator = new SagaOrchestrator<>(context);

        // Step 1: Create Transaction Entity (State: PENDING)
        orchestrator.addStep(new SagaStep<>() {
            @Override public String getName() { return "CreateTransaction"; }
            @Override public void execute(PaymentSagaContext ctx) {
                Transaction transaction = createPendingTransaction(
                        ctx.getSenderId(),
                        ctx.getReceiverId(),
                        ctx.getAmount(),
                        TransactionType.PAYMENT,
                        request.idempotencyKey()
                );
                ctx.setTransaction(transaction);
            }
            @Override public void compensate(PaymentSagaContext ctx) {
                if (ctx.getTransaction() != null) {
                    markFailed(ctx.getTransaction());
                    log.warn("Saga Compensate: Marked transaction {} as FAILED", ctx.getTransaction().getId());
                }
            }
        });

        // Step 2: Validate Users and Balance
        orchestrator.addStep(new SagaStep<>() {
            @Override public String getName() { return "ValidateAndReserve"; }
            @Override public void execute(PaymentSagaContext ctx) {
                validateUser(ctx.getSenderId());
                validateUser(ctx.getReceiverId());
                ensureSufficientBalance(ctx.getSenderId(), ctx.getAmount());
            }
            @Override public void compensate(PaymentSagaContext ctx) {
                // Read-only step, no compensation needed
            }
        });

        // Step 3: Ledger Accounting (Deduct/Credit)
        orchestrator.addStep(new SagaStep<>() {
            @Override public String getName() { return "AccountingEntries"; }
            @Override public void execute(PaymentSagaContext ctx) {
                saveLedgerPair(ctx.getTransaction(), ctx.getSenderId(), ctx.getReceiverId(), ctx.getAmount());
                markSuccess(ctx.getTransaction());
            }
            @Override public void compensate(PaymentSagaContext ctx) {
                // Compensation: Create reverse ledger entries if needed or just mark transaction failed
                // Since this is a simple demo, marking transaction FAILED in Step 1 compensation is often enough,
                // but in a true distributed saga, we'd send a 'Refund' command here.
                log.warn("Saga Compensate: Ledger entries would be reversed here for transaction {}", ctx.getTransaction().getId());
            }
        });

        // Step 4: External Reward Publishing (Cross-Service context)
        orchestrator.addStep(new SagaStep<>() {
            @Override public String getName() { return "PublishRewardEvent"; }
            @Override public void execute(PaymentSagaContext ctx) {
                // We simulate this being an essential part of the Saga
                publishRewardEvent(ctx.getSenderId(), ctx.getTransaction().getType(), ctx.getAmount());
                publishTransactionHistoryEvent(ctx.getTransaction());
            }
            @Override public void compensate(PaymentSagaContext ctx) {
                log.warn("Saga Compensate: Rewards rollback (if any) for transaction {}", ctx.getTransaction().getId());
            }
        });

        try {
            orchestrator.execute();
            log.info("Payment completed successfully via Saga: transactionId={}", context.getTransaction().getId());
            return TransactionResponse.from(context.getTransaction());
        } catch (Exception ex) {
            log.error("Payment failed in Saga: {}", ex.getMessage());
            // Orchestrator already called compensate()
            throw ex;
        }
        // --- Saga Implementation Ends ---
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
                                                 Long receiverId,
                                                 BigDecimal amount,
                                                 TransactionType type,
                                                 String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setSenderId(userId); // Fixed undefined 'senderId' – assuming sender is userId for initiating pending
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
                || !request.userId().equals(existing.getUserId())
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
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        StringBuilder sb = new StringBuilder();
        sb.append("------------------------------------------\n");
        sb.append("           NEXPAY TRANSACTION RECEIPT     \n");
        sb.append("------------------------------------------\n");
        sb.append("Transaction ID : ").append(t.getId()).append("\n");
        sb.append("External Ref   : ").append(t.getIdempotencyKey()).append("\n");
        sb.append("Date & Time    : ").append(t.getCreatedAt()).append("\n");
        sb.append("Type           : ").append(t.getType()).append("\n");
        sb.append("Status         : ").append(t.getStatus()).append("\n");
        sb.append("Amount         : INR ").append(t.getAmount()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("From Account   : ").append(t.getSenderId()).append("\n");
        sb.append("To Account     : ").append(t.getReceiverId()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("Thank you for using NexPay!\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] buildStatementCsv(Long userId, LocalDateTime from, LocalDateTime to) {
        validateUser(userId);
        
        if (from.plusDays(90).isBefore(to)) {
            throw new IllegalArgumentException("Date range cannot exceed 90 days");
        }

        // Fetch all transactions for the user to ensure data is returned.
        // Date filtering is often problematic due to timezone shifts; returning all is safer for "Statement".
        List<Transaction> txns = transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, userId);

        StringBuilder csv = new StringBuilder();
        csv.append("TransactionID,Date,Type,Amount,Status,SenderID,ReceiverID,Reference\n");
        for (Transaction t : txns) {
            csv.append(t.getId()).append(",")
               .append(t.getCreatedAt()).append(",")
               .append(t.getType()).append(",")
               .append(t.getAmount()).append(",")
               .append(t.getStatus()).append(",")
               .append(t.getSenderId()).append(",")
               .append(t.getReceiverId()).append(",")
               .append(t.getIdempotencyKey())
               .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] buildStatementPdf(Long userId, LocalDateTime from, LocalDateTime to) {
        validateStatementWindow(from, to);
        validateUser(userId);

        List<Transaction> txns = transactionRepository.findByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, userId)
                .stream()
                .filter(t -> (t.getCreatedAt().isAfter(from) || t.getCreatedAt().isEqual(from)) && 
                             (t.getCreatedAt().isBefore(to) || t.getCreatedAt().isEqual(to)))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                        NEXPAY ACCOUNT STATEMENT                        \n");
        sb.append("========================================================================\n");
        sb.append("User ID: ").append(userId).append("\n");
        sb.append("Period : ").append(from).append(" to ").append(to).append("\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("%-8s | %-19s | %-10s | %-10s | %-8s\n", "ID", "Date", "Type", "Amount", "Status"));
        sb.append("------------------------------------------------------------------------\n");

        for (Transaction t : txns) {
            sb.append(String.format("%-8d | %-19s | %-10s | %-10s | %-8s\n",
                    t.getId(),
                    t.getCreatedAt().toString().replace("T", " ").substring(0, 19),
                    t.getType(),
                    t.getAmount(),
                    t.getStatus()));
        }
        sb.append("========================================================================\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
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

