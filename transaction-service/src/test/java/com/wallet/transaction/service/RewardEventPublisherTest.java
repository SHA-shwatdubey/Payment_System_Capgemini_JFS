package com.wallet.transaction.service;

import com.wallet.transaction.dto.WalletEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RewardEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_sendsEventToConfiguredExchangeAndKey() {
        RewardEventPublisher publisher = new RewardEventPublisher(rabbitTemplate);
        WalletEvent event = new WalletEvent(1L, "TOPUP", new BigDecimal("10.00"));

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend("wallet.events.exchange", "wallet.events.key", event);
    }
}

