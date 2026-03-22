package com.wallet.auth.repository;

import com.wallet.auth.entity.AuthUser;
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
class AuthUserRepositoryTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Test
    void findByUsername_returnsSavedUser() {
        AuthUser user = new AuthUser(null, "repo-user", "pwd", "USER");
        authUserRepository.save(user);

        assertThat(authUserRepository.findByUsername("repo-user"))
                .isPresent()
                .get()
                .extracting(AuthUser::getRole)
                .isEqualTo("USER");
    }

    @Test
    void findByUsername_whenMissing_returnsEmpty() {
        assertThat(authUserRepository.findByUsername("missing-user")).isEmpty();
    }
}







