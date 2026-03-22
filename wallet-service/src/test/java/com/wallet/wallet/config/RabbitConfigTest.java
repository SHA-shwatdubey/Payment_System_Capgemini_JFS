package com.wallet.wallet.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private final RabbitConfig rabbitConfig = new RabbitConfig();

    @Test
    void walletExchange_buildsDirectExchange() {
        DirectExchange exchange = rabbitConfig.walletExchange();

        assertThat(exchange.getName()).isEqualTo(RabbitConfig.WALLET_EVENTS_EXCHANGE);
    }

    @Test
    void walletQueue_buildsDurableQueue() {
        Queue queue = rabbitConfig.walletQueue();

        assertThat(queue.getName()).isEqualTo(RabbitConfig.WALLET_EVENTS_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void walletBinding_connectsQueueAndExchange() {
        Queue queue = rabbitConfig.walletQueue();
        DirectExchange exchange = rabbitConfig.walletExchange();

        Binding binding = rabbitConfig.walletBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo(RabbitConfig.WALLET_EVENTS_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.WALLET_EVENTS_EXCHANGE);
    }
}

