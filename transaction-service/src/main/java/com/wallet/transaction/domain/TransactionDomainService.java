package com.wallet.transaction.domain;

import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.service.TransactionService;
import org.springframework.stereotype.Service;

@Service
public class TransactionDomainService {

    private final TransactionService transactionService;

    public TransactionDomainService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public TransactionResponse topup(TopupRequest request) {
        return transactionService.topup(request);
    }

    public TransactionResponse transfer(TransferRequest request) {
        return transactionService.transfer(request);
    }

    public TransactionResponse payment(PaymentRequest request) {
        return transactionService.payment(request);
    }

    public TransactionResponse refund(RefundRequest request) {
        return transactionService.refund(request);
    }
}

