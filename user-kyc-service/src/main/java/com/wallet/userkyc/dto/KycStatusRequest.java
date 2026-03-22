package com.wallet.userkyc.dto;

import jakarta.validation.constraints.NotBlank;

public record KycStatusRequest(
        @NotBlank(message = "status is required") String status,
        String reason
) {
}
