package com.example.deliveryservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.DeliveryStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeliveryEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventsPublisher.class);

    private final KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate;

    public DeliveryEventsPublisher(KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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

        kafkaTemplate.send(DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED, String.valueOf(orderId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} deliveryId={} orderId={} status={}",
                                DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED, deliveryId, orderId, status, ex);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.info("Published {} orderId={} status={} partition={} offset={} {}={}",
                                DeliveryKafkaTopics.DELIVERY_STATUS_CHANGED,
                                orderId,
                                status,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId);
                    }
                });
    }
}

