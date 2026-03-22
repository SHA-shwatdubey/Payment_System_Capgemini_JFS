package com.wallet.transaction.controller;

import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/topup")
    public TransactionResponse topup(@Valid @RequestBody TopupRequest request) {
        return transactionService.topup(request);
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }

    @PostMapping("/payment")
    public TransactionResponse payment(@Valid @RequestBody PaymentRequest request) {
        return transactionService.payment(request);
    }

    @PostMapping("/refund")
    public TransactionResponse refund(@Valid @RequestBody RefundRequest request) {
        return transactionService.refund(request);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable("id") Long id) {
        return transactionService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getByUser(@PathVariable("userId") Long userId) {
        return transactionService.getByUser(userId);
    }

    @GetMapping("/support/user/{userId}")
    public List<TransactionResponse> supportGetByUser(@PathVariable("userId") Long userId) {
        return transactionService.getByUser(userId);
    }

    @GetMapping("/history")
    public List<TransactionResponse> getHistory(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return transactionService.getHistory(from, to);
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(@PathVariable("id") Long id) {
        byte[] body = transactionService.buildReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    @GetMapping("/statement")
    public ResponseEntity<byte[]> statement(
            @RequestParam("userId") Long userId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "format", defaultValue = "PDF") String format
    ) {
        if ("CSV".equalsIgnoreCase(format)) {
            byte[] body = transactionService.buildStatementCsv(userId, from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement-" + userId + ".csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(body);
        }

        byte[] body = transactionService.buildStatementPdf(userId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement-" + userId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}



