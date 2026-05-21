package com.example.deliveryservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.DeliveryStatusChangedEvent;
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

    private final KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate;
    private final long timeoutMs;

    public DeliveryEventsPublisher(
            KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate,
            @Value("${app.kafka.send-timeout-ms:200}") long timeoutMs
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.timeoutMs = timeoutMs;
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

        try {
            kafkaTemplate.send(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED, String.valueOf(orderId), event)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            log.info("Published {} orderId={} status={} {}={}",
                    DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED,
                    orderId,
                    status,
                    CorrelationHeaders.CORRELATION_ID,
                    correlationId);
        } catch (Exception ex) {
            log.warn("Failed to publish {} deliveryId={} orderId={} status={} {}={} (non-fatal)",
                    DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED,
                    deliveryId,
                    orderId,
                    status,
                    CorrelationHeaders.CORRELATION_ID,
                    correlationId,
                    ex);
        }
    }
}
