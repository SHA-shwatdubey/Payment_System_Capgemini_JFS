package com.wallet.transaction.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void constants_areStable() {
        assertThat(RabbitConfig.WALLET_EVENTS_EXCHANGE).isEqualTo("wallet.events.exchange");
        assertThat(RabbitConfig.WALLET_EVENTS_KEY).isEqualTo("wallet.events.key");
    }
}

