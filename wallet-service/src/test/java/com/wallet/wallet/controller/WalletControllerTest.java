package com.wallet.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.wallet.dto.PaymentTopupConfirmResponse;
import com.wallet.wallet.dto.PaymentTopupInitRequest;
import com.wallet.wallet.dto.PaymentTopupInitResponse;
import com.wallet.wallet.dto.TopupRequest;
import com.wallet.wallet.dto.TransferRequest;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WalletController.class, properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.config.fail-fast=false",
                "spring.config.import=optional:configserver:",
                "eureka.client.enabled=false"
})
class WalletControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private WalletService walletService;

        @Test
        void getBalance_returnsBalance() throws Exception {
                when(walletService.getBalance(7L)).thenReturn(new BigDecimal("900.00"));

                mockMvc.perform(get("/api/wallet/balance").param("userId", "7"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.balance").value(900.00));
        }

        @Test
        void topup_returnsUpdatedAccount() throws Exception {
                WalletAccount account = new WalletAccount();
                account.setId(1L);
                account.setUserId(9L);
                account.setBalance(new BigDecimal("500.00"));
                when(walletService.topup(any(TopupRequest.class))).thenReturn(account);

                TopupRequest request = new TopupRequest(9L, new BigDecimal("500.00"), "UPI");
                mockMvc.perform(post("/api/wallet/topup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(9L));
        }

        @Test
        void transactions_returnsHistory() throws Exception {
                when(walletService.history(9L)).thenReturn(List.of());

                mockMvc.perform(get("/api/wallet/transactions").param("userId", "9"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        void initTopup_returnsGatewayResponse() throws Exception {
                when(walletService.initTopupPayment(any(PaymentTopupInitRequest.class)))
                                .thenReturn(new PaymentTopupInitResponse("pay-1", "CREATED", "https://pay"));

                mockMvc.perform(post("/api/wallet/topup/init")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PaymentTopupInitRequest(3L, new BigDecimal("55.00"), "UPI"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentRef").value("pay-1"));
        }

        @Test
        void confirmTopup_returnsConfirmationPayload() throws Exception {
                WalletAccount account = new WalletAccount();
                account.setUserId(3L);
                account.setBalance(new BigDecimal("155.00"));
                when(walletService.confirmTopupPayment("pay-2"))
                                .thenReturn(new PaymentTopupConfirmResponse("pay-2", "CAPTURED", account));

                mockMvc.perform(post("/api/wallet/topup/confirm/pay-2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentStatus").value("CAPTURED"))
                                .andExpect(jsonPath("$.walletAccount.userId").value(3L));
        }

        @Test
        void transfer_returnsSuccessMessage() throws Exception {
                when(walletService.transfer(any(TransferRequest.class))).thenReturn("Transfer successful");

                mockMvc.perform(post("/api/wallet/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new TransferRequest(1L, 2L, new BigDecimal("10.00")))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Transfer successful"));
        }

        @Test
        void limits_returnsConfig() throws Exception {
                WalletLimitConfig config = new WalletLimitConfig();
                config.setId(1L);
                config.setDailyTopupLimit(new BigDecimal("50000"));
                config.setDailyTransferLimit(new BigDecimal("25000"));
                config.setDailyTransferCountLimit(10);
                when(walletService.getLimits()).thenReturn(config);

                mockMvc.perform(get("/api/wallet/admin/limits"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L));
        }
}
