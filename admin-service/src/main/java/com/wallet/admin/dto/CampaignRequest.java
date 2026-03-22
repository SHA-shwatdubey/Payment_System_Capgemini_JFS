package com.wallet.admin.dto;

import java.time.LocalDate;

public record CampaignRequest(String name, String ruleType, Integer bonusPoints, LocalDate startDate, LocalDate endDate) {
}

