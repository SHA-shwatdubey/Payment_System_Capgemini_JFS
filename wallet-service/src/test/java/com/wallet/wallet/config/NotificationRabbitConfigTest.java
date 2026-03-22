package com.wallet.wallet.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRabbitConfigTest {

    private final NotificationRabbitConfig config = new NotificationRabbitConfig();

    @Test
    void notificationExchange_isDurableDirectExchange() {
        DirectExchange exchange = config.notificationExchange();

        assertThat(exchange.getName()).isEqualTo(NotificationRabbitConfig.NOTIFICATION_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void rabbitMessageConverter_returnsJacksonConverter() {
        assertThat(config.rabbitMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}

