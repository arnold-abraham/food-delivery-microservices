package com.example.deliveryservice.kafka;

import com.example.contracts.events.DeliveryStatusChangedEvent;
import com.example.contracts.events.RiderAssignedEvent;
import com.example.deliveryservice.outbox.OutboxEvent;
import com.example.deliveryservice.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeliveryEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventsPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public DeliveryEventsPublisher(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.enabled:true}") boolean enabled
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public void publishRiderAssigned(Long deliveryId, Long orderId, Long driverId, String correlationId) {
        RiderAssignedEvent event = new RiderAssignedEvent(
                RiderAssignedEvent.VERSION, orderId, deliveryId, driverId, Instant.now(), correlationId);
        save(DeliveryKafkaTopics.RIDER_ASSIGNED, String.valueOf(orderId), event);
    }

    public void publishStatusChanged(Long deliveryId, Long orderId, String status, String correlationId) {
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                DeliveryStatusChangedEvent.VERSION, deliveryId, orderId, status, Instant.now(), correlationId);
        save(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED, String.valueOf(orderId), event);
    }

    private void save(String topic, String key, Object event) {
        if (!enabled) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(topic, key, payload));
            log.debug("Queued outbox event topic={} key={}", topic, key);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize event for outbox topic={} key={}", topic, key, ex);
        }
    }
}
