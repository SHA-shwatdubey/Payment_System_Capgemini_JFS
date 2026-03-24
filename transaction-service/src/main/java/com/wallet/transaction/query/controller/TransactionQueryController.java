package com.wallet.transaction.query.controller;

import com.wallet.transaction.query.dto.TransactionReadDto;
import com.wallet.transaction.query.handler.TransactionQueryHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionQueryController {

    private final TransactionQueryHandler transactionQueryHandler;

    public TransactionQueryController(TransactionQueryHandler transactionQueryHandler) {
        this.transactionQueryHandler = transactionQueryHandler;
    }

    @GetMapping("/{userId}")
    public List<TransactionReadDto> getByUser(@PathVariable Long userId) {
        return transactionQueryHandler.getTransactionsByUserId(userId);
    }
}

