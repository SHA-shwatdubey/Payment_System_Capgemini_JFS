package com.wallet.rewards.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRabbitConfigTest {

    @Test
    void notificationRabbitBeans_areConfigured() {
        NotificationRabbitConfig config = new NotificationRabbitConfig();

        DirectExchange exchange = config.notificationExchange();

        assertThat(exchange.getName()).isEqualTo(NotificationRabbitConfig.NOTIFICATION_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(config.rabbitMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}

