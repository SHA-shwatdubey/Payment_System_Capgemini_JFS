package com.wallet.rewards.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void walletRabbitBeans_areConfigured() {
        RabbitConfig config = new RabbitConfig();

        DirectExchange exchange = config.walletExchange();
        Queue queue = config.walletQueue();
        Binding binding = config.walletBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo(RabbitConfig.WALLET_EVENTS_EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitConfig.WALLET_EVENTS_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.WALLET_EVENTS_KEY);
    }
}

