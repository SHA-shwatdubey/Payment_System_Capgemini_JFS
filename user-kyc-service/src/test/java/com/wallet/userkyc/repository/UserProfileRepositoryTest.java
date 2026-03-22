package com.wallet.userkyc.repository;

import com.wallet.userkyc.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false"
})
class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository repository;

    @Test
    void findByAuthUserId_returnsSavedProfile() {
        UserProfile profile = new UserProfile();
        profile.setAuthUserId(100L);
        profile.setFullName("Repo User");
        profile.setKycStatus("PENDING");
        repository.save(profile);

        assertThat(repository.findByAuthUserId(100L))
                .isPresent()
                .get()
                .extracting(UserProfile::getFullName)
                .isEqualTo("Repo User");
    }
}

