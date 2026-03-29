package com.wallet.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"eureka.client.enabled=false", "eureka.server.enabled=false"})
class EurekaServerApplicationTest {

    @Test
    void contextLoads() {
        // Basic smoke test to ensure Eureka application context starts
        assertThat(true).isTrue();
    }
}
