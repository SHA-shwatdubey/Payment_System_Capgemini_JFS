package com.wallet.wallet.service;

import com.wallet.wallet.dto.WalletEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publish_sendsEventToConfiguredExchangeAndKey() {
        WalletEventPublisher publisher = new WalletEventPublisher(rabbitTemplate);
        WalletEvent event = new WalletEvent(1L, "TOPUP", new BigDecimal("25.00"));

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend("wallet.events.exchange", "wallet.events.key", event);
    }
}

