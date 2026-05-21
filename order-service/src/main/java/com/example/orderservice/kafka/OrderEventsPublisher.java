package com.example.orderservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.OrderPlacedEvent;
import com.example.contracts.events.PaymentCompletedEvent;
import com.example.contracts.events.PaymentRequestedEvent;
import com.example.contracts.events.RiderAssignedEvent;
import com.example.contracts.topics.DeliveryKafkaTopics;
import com.example.contracts.topics.OrderKafkaTopics;
import com.example.contracts.topics.PaymentKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    public void publishPaymentRequested(long orderId, BigDecimal amount, BigDecimal expectedAmount) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                PaymentRequestedEvent.VERSION,
                orderId,
                amount,
                expectedAmount,
                Instant.now(),
                correlationId
        );

        send(PaymentKafkaTopics.PAYMENT_REQUESTED, String.valueOf(orderId), event, "orderId=" + orderId);
    }

    public void publishPaymentCompleted(long orderId, BigDecimal amount, BigDecimal expectedAmount, String status) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                PaymentCompletedEvent.VERSION,
                orderId,
                amount,
                expectedAmount,
                status,
                Instant.now(),
                correlationId
        );

        send(PaymentKafkaTopics.PAYMENT_COMPLETED, String.valueOf(orderId), event,
                "orderId=" + orderId + " status=" + status);
    }

    public void publishRiderAssigned(long orderId, long deliveryId, long driverId) {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        RiderAssignedEvent event = new RiderAssignedEvent(
                RiderAssignedEvent.VERSION,
                orderId,
                deliveryId,
                driverId,
                Instant.now(),
                correlationId
        );

        send(DeliveryKafkaTopics.RIDER_ASSIGNED, String.valueOf(orderId), event,
                "orderId=" + orderId + " deliveryId=" + deliveryId + " driverId=" + driverId);
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
