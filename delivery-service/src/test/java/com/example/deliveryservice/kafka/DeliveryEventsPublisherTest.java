package com.example.deliveryservice.kafka;

import com.example.deliveryservice.outbox.OutboxEvent;
import com.example.deliveryservice.outbox.OutboxRepository;
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

class DeliveryEventsPublisherTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private DeliveryEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        publisher = new DeliveryEventsPublisher(outboxRepository, objectMapper, true);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishRiderAssigned_savesOutboxEventWithCorrectTopic() throws Exception {
        publisher.publishRiderAssigned(10L, 20L, 30L, "corr-abc");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo(DeliveryKafkaTopics.RIDER_ASSIGNED);
        assertThat(saved.getEventKey()).isEqualTo("20");

        JsonNode node = objectMapper.readTree(saved.getPayload());
        assertThat(node.get("deliveryId").asLong()).isEqualTo(10L);
        assertThat(node.get("orderId").asLong()).isEqualTo(20L);
        assertThat(node.get("driverId").asLong()).isEqualTo(30L);
        assertThat(node.get("correlationId").asText()).isEqualTo("corr-abc");
    }

    @Test
    void publishStatusChanged_savesOutboxEventWithCorrectTopic() throws Exception {
        publisher.publishStatusChanged(5L, 7L, "PICKED_UP", "corr-xyz");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED);
        assertThat(saved.getEventKey()).isEqualTo("7");

        JsonNode node = objectMapper.readTree(saved.getPayload());
        assertThat(node.get("status").asText()).isEqualTo("PICKED_UP");
        assertThat(node.get("orderId").asLong()).isEqualTo(7L);
    }

    @Test
    void doesNotSaveWhenDisabled() {
        DeliveryEventsPublisher disabled = new DeliveryEventsPublisher(outboxRepository, objectMapper, false);

        disabled.publishRiderAssigned(1L, 2L, 3L, null);
        disabled.publishStatusChanged(1L, 2L, "PICKED_UP", null);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void gracefulOnSerializationFailure() throws Exception {
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        DeliveryEventsPublisher pub = new DeliveryEventsPublisher(outboxRepository, broken, true);

        assertThatNoException().isThrownBy(() -> pub.publishRiderAssigned(1L, 2L, 3L, null));
    }
}
