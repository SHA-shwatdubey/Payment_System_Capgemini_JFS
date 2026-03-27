package com.wallet.userkyc.service;

import com.wallet.userkyc.dto.KycVerifyRequest;
import com.wallet.userkyc.dto.KycStatusRequest;
import com.wallet.userkyc.entity.UserProfile;
import com.wallet.userkyc.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//Ye class business logic handle karegi
@Service
@Transactional
public class UserKycService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png");

    // Ye DB access object hai
    private final UserProfileRepository repository;
    private final IntegrationClient integrationClient;
    private final NotificationClient notificationClient;

    // Spring automatically repository inject karega

    public UserKycService(UserProfileRepository repository,
            IntegrationClient integrationClient,
            NotificationClient notificationClient) {
        this.repository = repository;
        this.integrationClient = integrationClient;
        this.notificationClient = notificationClient;
    }

    // New user create karna
    public UserProfile createUser(UserProfile user) {
        user.setKycStatus("NOT_SUBMITTED");
        UserProfile saved = repository.save(user);
        if (saved.getAuthUserId() == null) {
            saved.setAuthUserId(saved.getId());
            saved = repository.save(saved);
        }
        return saved;
    }

    // User apna KYC submit kare
    public UserProfile submitKyc(Long userId, String documentId) {
        UserProfile user = getOrCreateUser(userId);
        user.setKycDocumentId(documentId);
        user.setKycProviderStatus(verifyKycStatus(userId, documentId));
        user.setKycProviderRef(documentId);
        user.setKycStatus(STATUS_PENDING);
        return repository.save(user);
    }

    public UserProfile submitKycFile(Long userId, MultipartFile document, String fullName, String email, String phone) {
        validateKycDocument(document);

        UserProfile user = getOrCreateUser(userId);
        user.setKycDocumentId("DOC-" + UUID.randomUUID());
        user.setKycDocumentName(document.getOriginalFilename());
        user.setKycDocumentContentType(document.getContentType());
        user.setKycDocumentSize(document.getSize());

        if (fullName != null && !fullName.isBlank())
            user.setFullName(fullName);
        if (email != null && !email.isBlank())
            user.setEmail(email);
        if (phone != null && !phone.isBlank())
            user.setPhone(phone);

        try {
            user.setKycDocumentData(document.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded document");
        }
        user.setKycProviderStatus(verifyKycStatus(userId, user.getKycDocumentId()));
        user.setKycProviderRef(user.getKycDocumentId());
        user.setKycStatus(STATUS_PENDING);
        return repository.save(user);
    }

    private String verifyKycStatus(Long userId, String documentRef) {
        try {
            return integrationClient.verifyKyc(
                    IntegrationClient.INTERNAL_CALL_VALUE,
                    new KycVerifyRequest(userId, documentRef)) == null ? "UNKNOWN" : "VERIFIED";
        } catch (Exception ignored) {
            return "FAILED";
        }
    }

    private void validateKycDocument(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new IllegalArgumentException("document file is required");
        }

        String contentType = document.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only PDF, DOC, DOCX, JPG, JPEG, and PNG files are allowed");
        }
    }

    // Admin KYC approve/reject kare
    public UserProfile updateKycStatus(Long userId, KycStatusRequest request) {
        String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);

        UserProfile user = repository.findByAuthUserId(userId)
                .or(() -> repository.findById(userId)
                        .filter(profile -> profile.getAuthUserId() == null || userId.equals(profile.getAuthUserId())))
                .orElseThrow();

        if (user.getAuthUserId() == null) {
            user.setAuthUserId(userId);
        }

        user.setKycStatus(normalizedStatus);
        UserProfile saved = repository.save(user);

        Long notificationUserId = saved.getAuthUserId() == null ? saved.getId() : saved.getAuthUserId();
        notificationClient.sendSafe(
                notificationUserId,
                "KYC_STATUS_UPDATE",
                "EMAIL",
                saved.getEmail() == null ? ("user-" + notificationUserId) : saved.getEmail(),
                "Your KYC status is now: " + saved.getKycStatus());
        return saved;
    }

    // Single user fetch karna
    public UserProfile getUser(Long id) {
        return getOrCreateUser(id);
    }

    // Sirf pending users nikalna
    public List<UserProfile> pendingKyc() {
        return repository.findAll().stream().filter(u -> STATUS_PENDING.equals(u.getKycStatus())).toList();
    }

    private UserProfile getOrCreateUser(Long userId) {
        return repository.findByAuthUserId(userId).orElseGet(() -> createDefaultProfile(userId));
    }

    private UserProfile createDefaultProfile(Long userId) {
        UserProfile user = new UserProfile();
        user.setAuthUserId(userId);
        user.setFullName("User " + userId);
        user.setEmail("user" + userId + "@nexpay.local");
        user.setPhone("NA");
        user.setKycStatus(STATUS_NOT_SUBMITTED);
        return repository.save(user);
    }
}
