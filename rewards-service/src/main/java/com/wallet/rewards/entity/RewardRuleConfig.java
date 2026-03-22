package com.wallet.rewards.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reward_rule_config")
public class RewardRuleConfig {
    @Id
    private Long id;

    private Integer pointsPer100;
    private Integer goldThreshold;
    private Integer platinumThreshold;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPointsPer100() {
        return pointsPer100;
    }

    public void setPointsPer100(Integer pointsPer100) {
        this.pointsPer100 = pointsPer100;
    }

    public Integer getGoldThreshold() {
        return goldThreshold;
    }

    public void setGoldThreshold(Integer goldThreshold) {
        this.goldThreshold = goldThreshold;
    }

    public Integer getPlatinumThreshold() {
        return platinumThreshold;
    }

    public void setPlatinumThreshold(Integer platinumThreshold) {
        this.platinumThreshold = platinumThreshold;
    }
}

