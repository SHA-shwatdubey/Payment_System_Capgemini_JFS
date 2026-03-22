package com.wallet.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_limit_config")
public class WalletLimitConfig {

    @Id
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyTopupLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyTransferLimit;

    @Column(nullable = false)
    private Integer dailyTransferCountLimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getDailyTopupLimit() {
        return dailyTopupLimit;
    }

    public void setDailyTopupLimit(BigDecimal dailyTopupLimit) {
        this.dailyTopupLimit = dailyTopupLimit;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }

    public Integer getDailyTransferCountLimit() {
        return dailyTransferCountLimit;
    }

    public void setDailyTransferCountLimit(Integer dailyTransferCountLimit) {
        this.dailyTransferCountLimit = dailyTransferCountLimit;
    }
}

