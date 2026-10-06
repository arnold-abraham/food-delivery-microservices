package com.example.orderservice.kafka;

import com.example.contracts.events.OrderPaidEvent;
import com.example.contracts.events.OrderPlacedEvent;
import com.example.contracts.topics.OrderKafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrderEventsPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private OrderEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OrderEventsPublisher(kafkaTemplate, 200, true);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishOrderPlaced_sendsToCorrectTopic() {
        publisher.publishOrderPlaced(1L, 2L, 3L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(OrderKafkaTopics.ORDER_PLACED), eq("1"), captor.capture());

        OrderPlacedEvent event = (OrderPlacedEvent) captor.getValue();
        assertThat(event.orderId()).isEqualTo(1L);
        assertThat(event.userId()).isEqualTo(2L);
        assertThat(event.restaurantId()).isEqualTo(3L);
        assertThat(event.eventVersion()).isEqualTo(OrderPlacedEvent.VERSION);
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void publishOrderPaid_sendsToCorrectTopic() {
        publisher.publishOrderPaid(10L, 20L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(OrderKafkaTopics.ORDER_PAID), eq("10"), captor.capture());

        OrderPaidEvent event = (OrderPaidEvent) captor.getValue();
        assertThat(event.orderId()).isEqualTo(10L);
        assertThat(event.driverId()).isEqualTo(20L);
        assertThat(event.eventVersion()).isEqualTo(OrderPaidEvent.VERSION);
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void publishOrderPlaced_swallowsKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatNoException().isThrownBy(() -> publisher.publishOrderPlaced(1L, 2L, 3L));
    }

    @Test
    void publishOrderPaid_swallowsKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatNoException().isThrownBy(() -> publisher.publishOrderPaid(10L, 20L));
    }

    @Test
    void doesNotSendWhenDisabled() {
        OrderEventsPublisher disabled = new OrderEventsPublisher(kafkaTemplate, 200, false);

        disabled.publishOrderPlaced(1L, 2L, 3L);
        disabled.publishOrderPaid(10L, 20L);

        verifyNoInteractions(kafkaTemplate);
    }
}
