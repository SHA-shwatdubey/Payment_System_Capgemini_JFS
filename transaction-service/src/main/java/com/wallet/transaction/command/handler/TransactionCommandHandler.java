package com.wallet.transaction.command.handler;

import com.wallet.transaction.command.dto.CreateTransactionCommand;
import com.wallet.transaction.command.dto.TransactionCommandType;
import com.wallet.transaction.domain.TransactionDomainService;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.event.TransactionCreatedEvent;
import com.wallet.transaction.event.TransactionEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class TransactionCommandHandler {

    private final TransactionDomainService transactionDomainService;
    private final TransactionEventPublisher transactionEventPublisher;

    public TransactionCommandHandler(TransactionDomainService transactionDomainService,
                                     TransactionEventPublisher transactionEventPublisher) {
        this.transactionDomainService = transactionDomainService;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Transactional
    @CacheEvict(cacheNames = "transactionHistory", allEntries = true)
    public void handle(CreateTransactionCommand command) {
        TransactionResponse response = execute(command);
        transactionEventPublisher.publish(new TransactionCreatedEvent(
                command.idempotencyKey(),
                impactedUsers(response).stream().toList(),
                LocalDateTime.now()
        ));
    }

    private TransactionResponse execute(CreateTransactionCommand command) {
        if (command.type() == TransactionCommandType.TOPUP) {
            return transactionDomainService.topup(new TopupRequest(
                    command.userId(),
                    command.amount(),
                    command.idempotencyKey()
            ));
        }

        if (command.type() == TransactionCommandType.TRANSFER) {
            return transactionDomainService.transfer(new TransferRequest(
                    command.senderId(),
                    command.receiverId(),
                    command.amount(),
                    command.idempotencyKey()
            ));
        }

        if (command.type() == TransactionCommandType.PAYMENT) {
            return transactionDomainService.payment(new PaymentRequest(
                    command.senderId(),
                    command.receiverId(),
                    command.amount(),
                    command.idempotencyKey()
            ));
        }

        if (command.type() == TransactionCommandType.REFUND) {
            if (command.originalTransactionId() == null) {
                throw new IllegalArgumentException("originalTransactionId is required for REFUND command");
            }
            return transactionDomainService.refund(new RefundRequest(
                    command.senderId(),
                    command.receiverId(),
                    command.amount(),
                    command.originalTransactionId(),
                    command.idempotencyKey()
            ));
        }

        throw new IllegalArgumentException("Unsupported transaction command type: " + command.type());
    }

    private Set<Long> impactedUsers(TransactionResponse response) {
        Set<Long> users = new LinkedHashSet<>();
        if (response.userId() != null) {
            users.add(response.userId());
        }
        if (response.senderId() != null) {
            users.add(response.senderId());
        }
        if (response.receiverId() != null) {
            users.add(response.receiverId());
        }
        return users;
    }
}


