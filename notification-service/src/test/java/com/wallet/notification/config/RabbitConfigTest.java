package com.wallet.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void rabbitBeans_areConfiguredAsExpected() {
        RabbitConfig config = new RabbitConfig();

        DirectExchange exchange = config.notificationExchange();
        Queue queue = config.notificationQueue();
        Binding binding = config.notificationBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo(RabbitConfig.NOTIFICATION_EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitConfig.NOTIFICATION_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.NOTIFICATION_ROUTING_KEY);
        assertThat(config.rabbitMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}

