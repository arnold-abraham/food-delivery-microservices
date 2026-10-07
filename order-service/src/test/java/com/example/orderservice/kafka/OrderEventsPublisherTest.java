package com.example.orderservice.kafka;

import com.example.contracts.topics.OrderKafkaTopics;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventsPublisherTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private OrderEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher = new OrderEventsPublisher(outboxRepository, objectMapper, true);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishOrderPlaced_savesOutboxEventWithCorrectTopic() throws Exception {
        publisher.publishOrderPlaced(1L, 2L, 3L);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo(OrderKafkaTopics.ORDER_PLACED);
        assertThat(saved.getEventKey()).isEqualTo("1");

        JsonNode node = objectMapper.readTree(saved.getPayload());
        assertThat(node.get("orderId").asLong()).isEqualTo(1L);
        assertThat(node.get("userId").asLong()).isEqualTo(2L);
        assertThat(node.get("restaurantId").asLong()).isEqualTo(3L);
        assertThat(node.get("eventVersion").asInt()).isEqualTo(1);
    }

    @Test
    void publishOrderPaid_savesOutboxEventWithCorrectTopic() throws Exception {
        publisher.publishOrderPaid(10L, 20L);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo(OrderKafkaTopics.ORDER_PAID);
        assertThat(saved.getEventKey()).isEqualTo("10");

        JsonNode node = objectMapper.readTree(saved.getPayload());
        assertThat(node.get("orderId").asLong()).isEqualTo(10L);
        assertThat(node.get("driverId").asLong()).isEqualTo(20L);
    }

    @Test
    void doesNotSaveWhenDisabled() {
        OrderEventsPublisher disabled = new OrderEventsPublisher(outboxRepository, objectMapper, false);

        disabled.publishOrderPlaced(1L, 2L, 3L);
        disabled.publishOrderPaid(10L, 20L);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void publishOrderPlaced_gracefulOnSerializationFailure() throws Exception {
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        OrderEventsPublisher pub = new OrderEventsPublisher(outboxRepository, broken, true);

        assertThatNoException().isThrownBy(() -> pub.publishOrderPlaced(1L, 2L, 3L));
    }
}
