package com.wallet.userkyc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long authUserId;

    private String fullName;
    private String email;
    private String phone;
    private String kycStatus;
    private String kycDocumentId;
    private String kycDocumentName;
    private String kycDocumentContentType;
    private String kycProviderStatus;
    private String kycProviderRef;
    private Long kycDocumentSize;

    @Lob
    @JsonIgnore
    private byte[] kycDocumentData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(Long authUserId) {
        this.authUserId = authUserId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getKycDocumentId() {
        return kycDocumentId;
    }

    public void setKycDocumentId(String kycDocumentId) {
        this.kycDocumentId = kycDocumentId;
    }

    public String getKycDocumentName() {
        return kycDocumentName;
    }

    public void setKycDocumentName(String kycDocumentName) {
        this.kycDocumentName = kycDocumentName;
    }

    public String getKycDocumentContentType() {
        return kycDocumentContentType;
    }

    public void setKycDocumentContentType(String kycDocumentContentType) {
        this.kycDocumentContentType = kycDocumentContentType;
    }

    public String getKycProviderStatus() {
        return kycProviderStatus;
    }

    public void setKycProviderStatus(String kycProviderStatus) {
        this.kycProviderStatus = kycProviderStatus;
    }

    public String getKycProviderRef() {
        return kycProviderRef;
    }

    public void setKycProviderRef(String kycProviderRef) {
        this.kycProviderRef = kycProviderRef;
    }

    public Long getKycDocumentSize() {
        return kycDocumentSize;
    }

    public void setKycDocumentSize(Long kycDocumentSize) {
        this.kycDocumentSize = kycDocumentSize;
    }

    public byte[] getKycDocumentData() {
        return kycDocumentData;
    }

    public void setKycDocumentData(byte[] kycDocumentData) {
        this.kycDocumentData = kycDocumentData;
    }
}
