package com.wallet.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transaction.dto.TopupRequest;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.entity.Transaction;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TransactionController.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void topup_withValidPayload_returnsResponse() throws Exception {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setUserId(7L);
        tx.setSenderId(0L);
        tx.setReceiverId(7L);
        tx.setAmount(new BigDecimal("120.00"));
        tx.setType(TransactionType.TOPUP);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setCreatedAt(LocalDateTime.now());

        when(transactionService.topup(any(TopupRequest.class))).thenReturn(TransactionResponse.from(tx));

        TopupRequest request = new TopupRequest(7L, new BigDecimal("120.00"), "idem-1");
        mockMvc.perform(post("/transactions/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void topup_withInvalidPayload_returnsBadRequest() throws Exception {
        TopupRequest request = new TopupRequest(null, new BigDecimal("0"), "");

        mockMvc.perform(post("/transactions/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_whenServiceReturnsResult_returnsOk() throws Exception {
        Transaction tx = new Transaction();
        tx.setId(2L);
        tx.setUserId(1L);
        tx.setSenderId(1L);
        tx.setReceiverId(2L);
        tx.setAmount(new BigDecimal("20.00"));
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus(TransactionStatus.SUCCESS);
        when(transactionService.transfer(any(TransferRequest.class))).thenReturn(TransactionResponse.from(tx));

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(1L, 2L, new BigDecimal("20.00"), "idem-t"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"));
    }

    @Test
    void getByUser_returnsList() throws Exception {
        Transaction tx = new Transaction();
        tx.setId(3L);
        tx.setUserId(7L);
        tx.setSenderId(0L);
        tx.setReceiverId(7L);
        tx.setAmount(new BigDecimal("10.00"));
        tx.setType(TransactionType.TOPUP);
        tx.setStatus(TransactionStatus.SUCCESS);
        when(transactionService.getByUser(7L)).thenReturn(List.of(TransactionResponse.from(tx)));

        mockMvc.perform(get("/transactions/user/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L));
    }

    @Test
    void statement_withCsvFormat_returnsCsvHeaders() throws Exception {
        when(transactionService.buildStatementCsv(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn("a,b\n1,2".getBytes());

        mockMvc.perform(get("/transactions/statement")
                        .param("userId", "7")
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2025-01-02T00:00:00")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=statement-7.csv"))
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    void receipt_returnsPdfHeaders() throws Exception {
        when(transactionService.buildReceiptPdf(9L)).thenReturn("receipt".getBytes());

        mockMvc.perform(get("/transactions/9/receipt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=receipt-9.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }
}





