package com.example.orderservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.OrderPaidEvent;
import com.example.contracts.events.OrderPlacedEvent;
import com.example.contracts.topics.OrderKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class OrderEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long timeoutMs;
    private final boolean enabled;

    public OrderEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.send-timeout-ms:200}") long timeoutMs,
            @Value("${app.kafka.enabled:true}") boolean enabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.timeoutMs = timeoutMs;
        this.enabled = enabled;
    }

    public void publishOrderPlaced(long orderId, long userId, long restaurantId) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        OrderPlacedEvent event = new OrderPlacedEvent(
                OrderPlacedEvent.VERSION,
                orderId,
                userId,
                restaurantId,
                Instant.now(),
                correlationId
        );

        send(OrderKafkaTopics.ORDER_PLACED, String.valueOf(orderId), event, "orderId=" + orderId);
    }

    public void publishOrderPaid(long orderId, long driverId) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        OrderPaidEvent event = new OrderPaidEvent(
                OrderPaidEvent.VERSION,
                orderId,
                driverId,
                Instant.now(),
                correlationId
        );

        send(OrderKafkaTopics.ORDER_PAID, String.valueOf(orderId), event,
                "orderId=" + orderId + " driverId=" + driverId);
    }

    private void send(String topic, String key, Object event, String details) {
        if (!enabled) {
            return;
        }
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        try {
            // Bound the time we wait for metadata/ack so tests don't stall when Kafka isn't running.
            kafkaTemplate.send(topic, key, event)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            log.info("Published {} {} {}={}", topic, details, CorrelationHeaders.CORRELATION_ID, correlationId);
        } catch (Exception ex) {
            log.warn("Failed to publish {} {} {}={} (non-fatal)",
                    topic, details, CorrelationHeaders.CORRELATION_ID, correlationId, ex);
        }
    }
}
