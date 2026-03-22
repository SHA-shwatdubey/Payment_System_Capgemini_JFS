package com.wallet.wallet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String WALLET_EVENTS_EXCHANGE = "wallet.events.exchange";
    public static final String WALLET_EVENTS_QUEUE = "wallet.events.queue";
    public static final String WALLET_EVENTS_KEY = "wallet.events.key";

    @Bean
    public DirectExchange walletExchange() {
        return new DirectExchange(WALLET_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue walletQueue() {
        return new Queue(WALLET_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding walletBinding(Queue walletQueue, DirectExchange walletExchange) {
        return BindingBuilder.bind(walletQueue).to(walletExchange).with(WALLET_EVENTS_KEY);
    }
}

