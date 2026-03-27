package com.wallet.transaction.service;

import com.wallet.transaction.dto.WalletEvent;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.repository.TransactionRepository;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.LedgerEntry;
import com.wallet.transaction.entity.EntryType;
import com.wallet.transaction.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WalletEventListener {
    private static final Logger log = LoggerFactory.getLogger(WalletEventListener.class);
    private static final Long SYSTEM_ACCOUNT_ID = 0L;

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletEventListener(TransactionRepository transactionRepository,
                               LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @RabbitListener(queues = "wallet.events.queue")
    @Transactional
    public void handleWalletEvent(WalletEvent event) {
        try {
            if (event == null || event.userId() == null) {
                log.warn("Received invalid wallet event: {}", event);
                return;
            }

            log.info("Processing wallet event: eventType={}, userId={}, amount={}", 
                    event.eventType(), event.userId(), event.amount());

            // Create transaction record based on event type (clean up suffixes like _DEBIT)
            String rawType = event.eventType() != null ? event.eventType().replace("_DEBIT", "").replace("_CREDIT", "") : "TRANSFER";
            TransactionType transactionType;
            try {
                transactionType = TransactionType.valueOf(rawType);
            } catch (Exception e) {
                transactionType = TransactionType.TRANSFER;
            }
            
            Transaction transaction = new Transaction();
            transaction.setUserId(event.userId());
            transaction.setAmount(event.amount());
            transaction.setType(transactionType);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setIdempotencyKey(UUID.randomUUID().toString());

            // Set sender and receiver based on transaction type
            if (transactionType == TransactionType.TOPUP) {
                transaction.setSenderId(SYSTEM_ACCOUNT_ID);
                transaction.setReceiverId(event.userId());
            } else {
                // For transfer, payment, refund
                transaction.setSenderId(event.userId());
                transaction.setReceiverId(event.userId());
            }

            transaction = transactionRepository.save(transaction);

            // Create ledger entries
            if (transactionType == TransactionType.TOPUP) {
                // Credit to user's account
                LedgerEntry credit = new LedgerEntry();
                credit.setTransaction(transaction);
                credit.setUserId(event.userId());
                credit.setEntryType(EntryType.CREDIT);
                credit.setAmount(event.amount());
                ledgerEntryRepository.save(credit);
            } else {
                // For other types, create both debit and credit entries
                LedgerEntry debit = new LedgerEntry();
                debit.setTransaction(transaction);
                debit.setUserId(transaction.getSenderId());
                debit.setEntryType(EntryType.DEBIT);
                debit.setAmount(event.amount());
                ledgerEntryRepository.save(debit);

                LedgerEntry credit = new LedgerEntry();
                credit.setTransaction(transaction);
                credit.setUserId(transaction.getReceiverId());
                credit.setEntryType(EntryType.CREDIT);
                credit.setAmount(event.amount());
                ledgerEntryRepository.save(credit);
            }

            log.info("Successfully created transaction record from wallet event: transactionId={}", 
                    transaction.getId());
        } catch (Exception e) {
            log.error("Error processing wallet event: {}", event, e);
            // Don't re-throw to prevent message being rejected and requeued infinitely
        }
    }
}

