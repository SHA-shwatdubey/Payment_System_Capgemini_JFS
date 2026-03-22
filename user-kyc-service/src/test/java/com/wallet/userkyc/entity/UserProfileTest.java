package com.wallet.userkyc.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileTest {

    @Test
    void gettersAndSetters_workForAllFields() {
        byte[] data = new byte[]{1, 2, 3};

        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setAuthUserId(9L);
        profile.setFullName("Demo User");
        profile.setEmail("demo@example.com");
        profile.setPhone("9999999999");
        profile.setKycStatus("PENDING");
        profile.setKycDocumentId("DOC-1");
        profile.setKycDocumentName("doc.pdf");
        profile.setKycDocumentContentType("application/pdf");
        profile.setKycProviderStatus("VERIFIED");
        profile.setKycProviderRef("ref-1");
        profile.setKycDocumentSize(123L);
        profile.setKycDocumentData(data);

        assertThat(profile.getId()).isEqualTo(1L);
        assertThat(profile.getAuthUserId()).isEqualTo(9L);
        assertThat(profile.getFullName()).isEqualTo("Demo User");
        assertThat(profile.getEmail()).isEqualTo("demo@example.com");
        assertThat(profile.getPhone()).isEqualTo("9999999999");
        assertThat(profile.getKycStatus()).isEqualTo("PENDING");
        assertThat(profile.getKycDocumentId()).isEqualTo("DOC-1");
        assertThat(profile.getKycDocumentName()).isEqualTo("doc.pdf");
        assertThat(profile.getKycDocumentContentType()).isEqualTo("application/pdf");
        assertThat(profile.getKycProviderStatus()).isEqualTo("VERIFIED");
        assertThat(profile.getKycProviderRef()).isEqualTo("ref-1");
        assertThat(profile.getKycDocumentSize()).isEqualTo(123L);
        assertThat(profile.getKycDocumentData()).containsExactly(1, 2, 3);
    }
}

