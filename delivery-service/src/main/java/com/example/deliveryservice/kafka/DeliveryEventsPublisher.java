package com.example.deliveryservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.DeliveryStatusChangedEvent;
import com.example.contracts.events.RiderAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class DeliveryEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventsPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long timeoutMs;

    public DeliveryEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.send-timeout-ms:200}") long timeoutMs
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.timeoutMs = timeoutMs;
    }

    public void publishRiderAssigned(Long deliveryId, Long orderId, Long driverId, String correlationId) {
        RiderAssignedEvent event = new RiderAssignedEvent(
                RiderAssignedEvent.VERSION,
                orderId,
                deliveryId,
                driverId,
                Instant.now(),
                correlationId
        );

        send(DeliveryKafkaTopics.RIDER_ASSIGNED, String.valueOf(orderId), event,
                "orderId=" + orderId + " deliveryId=" + deliveryId + " driverId=" + driverId,
                correlationId);
    }

    public void publishStatusChanged(Long deliveryId, Long orderId, String status, String correlationId) {
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                DeliveryStatusChangedEvent.VERSION,
                deliveryId,
                orderId,
                status,
                Instant.now(),
                correlationId
        );

        send(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED, String.valueOf(orderId), event,
                "orderId=" + orderId + " status=" + status,
                correlationId);
    }

    private void send(String topic, String key, Object event, String details, String correlationId) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            log.info("Published {} {} {}={}",
                    topic, details, CorrelationHeaders.CORRELATION_ID, correlationId);
        } catch (Exception ex) {
            log.warn("Failed to publish {} {} {}={} (non-fatal)",
                    topic, details, CorrelationHeaders.CORRELATION_ID, correlationId, ex);
        }
    }
}
