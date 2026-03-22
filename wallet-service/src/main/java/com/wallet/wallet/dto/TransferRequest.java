package com.wallet.wallet.dto;

import java.math.BigDecimal;

public record TransferRequest(Long fromUserId, Long toUserId, BigDecimal amount) {
}

