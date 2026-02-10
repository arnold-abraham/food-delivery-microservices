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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OrderEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventsPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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

        kafkaTemplate.send(OrderKafkaTopics.ORDER_PLACED, String.valueOf(orderId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} orderId={} {}={}",
                                OrderKafkaTopics.ORDER_PLACED,
                                orderId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId,
                                ex);
                    } else {
                        log.info("Published {} orderId={} {}={}",
                                OrderKafkaTopics.ORDER_PLACED,
                                orderId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId);
                    }
                });
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

        kafkaTemplate.send(PaymentKafkaTopics.PAYMENT_REQUESTED, String.valueOf(orderId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} orderId={} {}={}",
                                PaymentKafkaTopics.PAYMENT_REQUESTED,
                                orderId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId,
                                ex);
                    } else {
                        log.info("Published {} orderId={} {}={}",
                                PaymentKafkaTopics.PAYMENT_REQUESTED,
                                orderId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId);
                    }
                });
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

        kafkaTemplate.send(PaymentKafkaTopics.PAYMENT_COMPLETED, String.valueOf(orderId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} orderId={} status={} {}={}",
                                PaymentKafkaTopics.PAYMENT_COMPLETED,
                                orderId,
                                status,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId,
                                ex);
                    } else {
                        log.info("Published {} orderId={} status={} {}={}",
                                PaymentKafkaTopics.PAYMENT_COMPLETED,
                                orderId,
                                status,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId);
                    }
                });
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

        kafkaTemplate.send(DeliveryKafkaTopics.RIDER_ASSIGNED, String.valueOf(orderId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} orderId={} deliveryId={} driverId={} {}={}",
                                DeliveryKafkaTopics.RIDER_ASSIGNED,
                                orderId,
                                deliveryId,
                                driverId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId,
                                ex);
                    } else {
                        log.info("Published {} orderId={} deliveryId={} driverId={} {}={}",
                                DeliveryKafkaTopics.RIDER_ASSIGNED,
                                orderId,
                                deliveryId,
                                driverId,
                                CorrelationHeaders.CORRELATION_ID,
                                correlationId);
                    }
                });
    }
}
