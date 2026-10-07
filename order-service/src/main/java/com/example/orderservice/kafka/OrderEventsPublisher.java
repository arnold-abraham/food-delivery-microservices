package com.example.orderservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.OrderPaidEvent;
import com.example.contracts.events.OrderPlacedEvent;
import com.example.contracts.topics.OrderKafkaTopics;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public OrderEventsPublisher(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.enabled:true}") boolean enabled
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public void publishOrderPlaced(long orderId, long userId, long restaurantId) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        OrderPlacedEvent event = new OrderPlacedEvent(
                OrderPlacedEvent.VERSION, orderId, userId, restaurantId, Instant.now(), correlationId);
        save(OrderKafkaTopics.ORDER_PLACED, String.valueOf(orderId), event);
    }

    public void publishOrderPaid(long orderId, long driverId) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        OrderPaidEvent event = new OrderPaidEvent(
                OrderPaidEvent.VERSION, orderId, driverId, Instant.now(), correlationId);
        save(OrderKafkaTopics.ORDER_PAID, String.valueOf(orderId), event);
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
