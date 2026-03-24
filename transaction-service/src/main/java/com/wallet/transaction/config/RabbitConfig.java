package com.wallet.transaction.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String WALLET_EVENTS_EXCHANGE = "wallet.events.exchange";
    public static final String WALLET_EVENTS_KEY = "wallet.events.key";
    public static final String WALLET_EVENTS_QUEUE = "wallet.events.queue";

    public static final String CQRS_EXCHANGE = "cqrs.events.exchange";
    public static final String TRANSACTION_EVENTS_KEY = "transaction.events";
    public static final String TRANSACTION_EVENTS_QUEUE = "transaction.events";

    @Bean
    public DirectExchange walletExchange() {
        return new DirectExchange(WALLET_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue walletEventsQueue() {
        return new Queue(WALLET_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding walletEventsBinding(Queue walletEventsQueue, DirectExchange walletExchange) {
        return BindingBuilder.bind(walletEventsQueue)
                .to(walletExchange)
                .with(WALLET_EVENTS_KEY);
    }

    @Bean
    public DirectExchange transactionCqrsExchange() {
        return new DirectExchange(CQRS_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionEventsQueue() {
        return new Queue(TRANSACTION_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding transactionEventsBinding(Queue transactionEventsQueue, DirectExchange transactionCqrsExchange) {
        return BindingBuilder.bind(transactionEventsQueue)
                .to(transactionCqrsExchange)
                .with(TRANSACTION_EVENTS_KEY);
    }

    @Bean
    public MessageConverter transactionRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean(name = "rabbitTemplate")
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.core.RabbitTemplate template = 
                new org.springframework.amqp.rabbit.core.RabbitTemplate(connectionFactory);
        template.setMessageConverter(transactionRabbitMessageConverter());
        return template;
    }
}





