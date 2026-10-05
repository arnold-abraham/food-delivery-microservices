package com.example.deliveryservice.kafka;

import com.example.contracts.events.RiderAssignedEvent;
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

class DeliveryEventsPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private DeliveryEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DeliveryEventsPublisher(kafkaTemplate, 200);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishRiderAssigned_sendsToCorrectTopic() {
        publisher.publishRiderAssigned(10L, 20L, 30L, "corr-abc");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(DeliveryKafkaTopics.RIDER_ASSIGNED), eq("20"), eventCaptor.capture());

        RiderAssignedEvent event = (RiderAssignedEvent) eventCaptor.getValue();
        assertThat(event.deliveryId()).isEqualTo(10L);
        assertThat(event.orderId()).isEqualTo(20L);
        assertThat(event.driverId()).isEqualTo(30L);
        assertThat(event.correlationId()).isEqualTo("corr-abc");
        assertThat(event.eventVersion()).isEqualTo(RiderAssignedEvent.VERSION);
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void publishRiderAssigned_swallowsKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatNoException().isThrownBy(() ->
                publisher.publishRiderAssigned(1L, 2L, 3L, null));
    }

    @Test
    void publishStatusChanged_sendsToCorrectTopic() {
        publisher.publishStatusChanged(10L, 20L, "PICKED_UP", "corr-xyz");

        verify(kafkaTemplate).send(eq(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED), eq("20"), any());
    }
}
