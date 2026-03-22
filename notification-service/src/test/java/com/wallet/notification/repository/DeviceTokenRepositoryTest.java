package com.wallet.notification.repository;

import com.wallet.notification.entity.DeviceToken;
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
class DeviceTokenRepositoryTest {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void findByUserId_returnsToken() {
        DeviceToken token = new DeviceToken();
        token.setUserId(70L);
        token.setToken("abc-123");
        deviceTokenRepository.save(token);

        assertThat(deviceTokenRepository.findByUserId(70L)).isPresent();
    }
}




