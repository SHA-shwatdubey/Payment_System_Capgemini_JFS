package com.wallet.userkyc.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRabbitConfigTest {

    @Test
    void notificationExchange_createsDurableExchange() {
        NotificationRabbitConfig config = new NotificationRabbitConfig();

        DirectExchange exchange = config.notificationExchange();

        assertThat(exchange.getName()).isEqualTo(NotificationRabbitConfig.NOTIFICATION_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void rabbitMessageConverter_returnsJacksonConverter() {
        NotificationRabbitConfig config = new NotificationRabbitConfig();

        assertThat(config.rabbitMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}

