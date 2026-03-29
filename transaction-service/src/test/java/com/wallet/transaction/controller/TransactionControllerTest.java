package com.wallet.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transaction.dto.PaymentRequest;
import com.wallet.transaction.dto.RefundRequest;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void topup_returnsTransactionResponse() throws Exception {
        TopupRequest req = new TopupRequest(1L, new BigDecimal("100.00"), "idemp-1");
        TransactionResponse resp = new TransactionResponse(1L, 1L, 0L, 1L, new BigDecimal("100.00"), TransactionType.TOPUP, TransactionStatus.SUCCESS, "idemp-1", LocalDateTime.now());
        when(transactionService.topup(any())).thenReturn(resp);

        mockMvc.perform(post("/transactions/topup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void transfer_returnsTransactionResponse() throws Exception {
        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("50.00"), "idemp-2");
        TransactionResponse resp = new TransactionResponse(2L, 1L, 1L, 2L, new BigDecimal("50.00"), TransactionType.TRANSFER, TransactionStatus.SUCCESS, "idemp-2", LocalDateTime.now());
        when(transactionService.transfer(any())).thenReturn(resp);

        mockMvc.perform(post("/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void payment_returnsTransactionResponse() throws Exception {
        PaymentRequest req = new PaymentRequest(1L, 3L, new BigDecimal("20.00"), "idemp-3");
        TransactionResponse resp = new TransactionResponse(3L, 1L, 1L, 3L, new BigDecimal("20.00"), TransactionType.PAYMENT, TransactionStatus.SUCCESS, "idemp-3", LocalDateTime.now());
        when(transactionService.payment(any())).thenReturn(resp);

        mockMvc.perform(post("/transactions/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(20.00));
    }

    @Test
    void refund_returnsTransactionResponse() throws Exception {
        RefundRequest req = new RefundRequest(1L, 2L, new BigDecimal("10.00"), 3L, "idemp-refund");
        TransactionResponse resp = new TransactionResponse(4L, 2L, 2L, 1L, new BigDecimal("10.00"), TransactionType.REFUND, TransactionStatus.SUCCESS, "idemp-refund", LocalDateTime.now());
        when(transactionService.refund(any())).thenReturn(resp);

        mockMvc.perform(post("/transactions/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returnsResponse() throws Exception {
        TransactionResponse resp = new TransactionResponse(10L, 1L, 1L, 3L, new BigDecimal("15.00"), TransactionType.PAYMENT, TransactionStatus.SUCCESS, "key-10", LocalDateTime.now());
        when(transactionService.getById(10L)).thenReturn(resp);

        mockMvc.perform(get("/transactions/id/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getByUser_returnsList() throws Exception {
        when(transactionService.getByUser(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getHistory_returnsList() throws Exception {
        String from = "2023-01-01T00:00:00";
        String to = "2023-01-02T00:00:00";
        when(transactionService.getHistory(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions/history")
                .param("from", from)
                .param("to", to))
                .andExpect(status().isOk());
    }

    @Test
    void receipt_returnsPdf() throws Exception {
        when(transactionService.buildReceiptPdf(1L)).thenReturn("PDF-CONTENT".getBytes());

        mockMvc.perform(get("/transactions/1/receipt"))
                .andExpect(status().isOk())
                .andExpect(status().isOk());
    }

    @Test
    void statement_returnsCsv() throws Exception {
        when(transactionService.buildStatementCsv(any(), any(), any())).thenReturn("CSV-CONTENT".getBytes());

        mockMvc.perform(get("/transactions/statement")
                .param("userId", "1")
                .param("from", "2023-01-01T00:00:00")
                .param("to", "2023-01-02T00:00:00")
                .param("format", "CSV"))
                .andExpect(status().isOk());
    }
}
