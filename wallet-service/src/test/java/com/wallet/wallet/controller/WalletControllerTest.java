package com.wallet.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.wallet.dto.PaymentTopupConfirmResponse;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
import com.wallet.wallet.dto.WalletLimitUpdateRequest;
import com.wallet.wallet.entity.WalletAccount;
import com.wallet.wallet.entity.WalletLimitConfig;
import com.wallet.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getBalance_returnsBalance() throws Exception {
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(get("/api/wallet/balance").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void topup_returnsAccount() throws Exception {
        TopupRequest req = new TopupRequest(1L, new BigDecimal("50.00"), "UPI");
        WalletAccount account = new WalletAccount();
        account.setUserId(1L);
        account.setBalance(new BigDecimal("150.00"));
        when(walletService.topup(any())).thenReturn(account);

        mockMvc.perform(post("/api/wallet/topup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void initTopup_returnsInitResponse() throws Exception {
        PaymentTopupInitRequest req = new PaymentTopupInitRequest(1L, new BigDecimal("20.00"), "UPI");
        PaymentTopupInitResponse resp = new PaymentTopupInitResponse("PAY-REF-1", "PENDING", "http://mock-pay.local");
        when(walletService.initTopupPayment(any())).thenReturn(resp);

        mockMvc.perform(post("/api/wallet/topup/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentRef").value("PAY-REF-1"));
    }

    @Test
    void confirmTopup_returnsConfirmResponse() throws Exception {
        WalletAccount account = new WalletAccount();
        account.setUserId(1L);
        account.setBalance(new BigDecimal("200.00"));
        PaymentTopupConfirmResponse resp = new PaymentTopupConfirmResponse("PAY-REF-1", "CAPTURED", account);
        when(walletService.confirmTopupPayment("PAY-REF-1")).thenReturn(resp);

        mockMvc.perform(post("/api/wallet/topup/confirm/PAY-REF-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CAPTURED"));
    }

    @Test
    void transfer_returnsMessage() throws Exception {
        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("10.00"));
        when(walletService.transfer(any())).thenReturn("Transfer successful");

        mockMvc.perform(post("/api/wallet/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"));
    }

    @Test
    void transactions_returnsList() throws Exception {
        when(walletService.history(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/wallet/transactions").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void limits_returnsConfig() throws Exception {
        WalletLimitConfig config = new WalletLimitConfig(1L, new BigDecimal("50000"), new BigDecimal("25000"), 10);
        when(walletService.getLimits()).thenReturn(config);

        mockMvc.perform(get("/api/wallet/admin/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyTopupLimit").value(50000));
    }

    @Test
    void updateLimits_returnsConfig() throws Exception {
        WalletLimitUpdateRequest req = new WalletLimitUpdateRequest(new BigDecimal("1000"), new BigDecimal("500"), 5);
        WalletLimitConfig config = new WalletLimitConfig(1L, new BigDecimal("1000"), new BigDecimal("500"), 5);
        when(walletService.updateLimits(any())).thenReturn(config);

        mockMvc.perform(post("/api/wallet/admin/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
