package com.wallet.transaction.command.controller;

import com.wallet.transaction.command.dto.CommandStatusResponse;
import com.wallet.transaction.command.dto.CreateTransactionCommand;
import com.wallet.transaction.command.handler.TransactionCommandHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionCommandController {

    private final TransactionCommandHandler transactionCommandHandler;

    public TransactionCommandController(TransactionCommandHandler transactionCommandHandler) {
        this.transactionCommandHandler = transactionCommandHandler;
    }

    @PostMapping
    public ResponseEntity<CommandStatusResponse> create(@Valid @RequestBody CreateTransactionCommand command) {
        transactionCommandHandler.handle(command);
        return ResponseEntity.accepted().body(
                CommandStatusResponse.accepted(command.idempotencyKey(), "Transaction command accepted")
        );
    }
}

