package com.wallet.admin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_actions")
public class AdminAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     Example values:
    "CREATE_CAMPAIGN"
    "APPROVE_KYC"
    "REJECT_KYC"
     */
    private String actionType;

// targetId = us object (user/campaign/etc.) ka ID jis par admin ne action liya
    private Long targetId;  // Admin ne kis object pe action liya


    private String status;

    /*
    Action ka result kya hai
    Example:
    SUCCESS
    FAILED
    PENDING
     */
    private String reason;// Agar action reject hua ya fail hua → reason store hoga


    private LocalDateTime createdAt;  // Action kab hua

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

