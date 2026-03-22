package com.wallet.userkyc.service;

import com.wallet.userkyc.dto.KycStatusRequest;
import com.wallet.userkyc.dto.KycVerifyResponse;
import com.wallet.userkyc.entity.UserProfile;
import com.wallet.userkyc.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserKycServiceTest {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private IntegrationClient integrationClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private UserKycService userKycService;

    @Test
    void createUser_setsDefaultStatusAndAuthIdWhenMissing() {
        UserProfile input = new UserProfile();
        input.setFullName("Alice");

        UserProfile firstSave = new UserProfile();
        firstSave.setId(11L);
        firstSave.setKycStatus("NOT_SUBMITTED");
        firstSave.setAuthUserId(null);

        UserProfile secondSave = new UserProfile();
        secondSave.setId(11L);
        secondSave.setKycStatus("NOT_SUBMITTED");
        secondSave.setAuthUserId(11L);

        when(repository.save(any(UserProfile.class))).thenReturn(firstSave, secondSave);

        UserProfile saved = userKycService.createUser(input);

        assertThat(saved.getKycStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(saved.getAuthUserId()).isEqualTo(11L);
        verify(repository, times(2)).save(any(UserProfile.class));
    }

    @Test
    void submitKyc_marksPendingAndVerified() {
        UserProfile existing = new UserProfile();
        existing.setAuthUserId(7L);

        when(repository.findByAuthUserId(7L)).thenReturn(Optional.of(existing));
        when(integrationClient.verifyKyc(eq(IntegrationClient.INTERNAL_CALL_VALUE), any()))
                .thenReturn(new KycVerifyResponse("r1", "VERIFIED", "ok"));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = userKycService.submitKyc(7L, "DOC-9");

        assertThat(saved.getKycStatus()).isEqualTo("PENDING");
        assertThat(saved.getKycProviderStatus()).isEqualTo("VERIFIED");
        assertThat(saved.getKycProviderRef()).isEqualTo("DOC-9");
    }

    @Test
    void submitKyc_whenIntegrationFails_setsFailedProviderStatus() {
        UserProfile existing = new UserProfile();
        existing.setAuthUserId(9L);

        when(repository.findByAuthUserId(9L)).thenReturn(Optional.of(existing));
        when(integrationClient.verifyKyc(eq(IntegrationClient.INTERNAL_CALL_VALUE), any()))
                .thenThrow(new RuntimeException("down"));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = userKycService.submitKyc(9L, "DOC-X");

        assertThat(saved.getKycProviderStatus()).isEqualTo("FAILED");
        assertThat(saved.getKycStatus()).isEqualTo("PENDING");
    }

    @Test
    void submitKycFile_withInvalidType_throwsError() {
        MockMultipartFile file = new MockMultipartFile("document", "a.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> userKycService.submitKycFile(3L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF");
    }

    @Test
    void submitKycFile_withValidPdf_savesMetadata() {
        MockMultipartFile file = new MockMultipartFile("document", "id.pdf", "application/pdf", "pdf-content".getBytes());

        UserProfile existing = new UserProfile();
        existing.setAuthUserId(4L);

        when(repository.findByAuthUserId(4L)).thenReturn(Optional.of(existing));
        when(integrationClient.verifyKyc(eq(IntegrationClient.INTERNAL_CALL_VALUE), any()))
                .thenReturn(new KycVerifyResponse("r2", "VERIFIED", "ok"));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = userKycService.submitKycFile(4L, file);

        assertThat(saved.getKycStatus()).isEqualTo("PENDING");
        assertThat(saved.getKycDocumentName()).isEqualTo("id.pdf");
        assertThat(saved.getKycDocumentContentType()).isEqualTo("application/pdf");
        assertThat(saved.getKycDocumentData()).isNotNull();
    }

    @Test
    void updateKycStatus_sendsNotification() {
        UserProfile existing = new UserProfile();
        existing.setId(30L);
        existing.setAuthUserId(40L);
        existing.setEmail("user40@example.com");

        when(repository.findByAuthUserId(40L)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = userKycService.updateKycStatus(40L, new KycStatusRequest("APPROVED", "good"));

        assertThat(saved.getKycStatus()).isEqualTo("APPROVED");
        verify(notificationClient).sendSafe(40L, "KYC_STATUS_UPDATE", "EMAIL", "user40@example.com",
                "Your KYC status is now: APPROVED");
    }

    @Test
    void pendingKyc_returnsOnlyPendingUsers() {
        UserProfile pending = new UserProfile();
        pending.setKycStatus("PENDING");

        UserProfile approved = new UserProfile();
        approved.setKycStatus("APPROVED");

        when(repository.findAll()).thenReturn(List.of(pending, approved));

        List<UserProfile> result = userKycService.pendingKyc();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKycStatus()).isEqualTo("PENDING");
    }

    @Test
    void getUser_whenMissing_createsDefaultProfile() {
        UserProfile created = new UserProfile();
        created.setAuthUserId(88L);
        created.setKycStatus("NOT_SUBMITTED");

        when(repository.findByAuthUserId(88L)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenReturn(created);

        UserProfile user = userKycService.getUser(88L);

        assertThat(user.getAuthUserId()).isEqualTo(88L);
        assertThat(user.getKycStatus()).isEqualTo("NOT_SUBMITTED");
    }
}


