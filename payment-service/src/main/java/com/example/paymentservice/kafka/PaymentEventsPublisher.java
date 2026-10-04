package com.example.paymentservice.kafka;

import com.example.contracts.CorrelationHeaders;
import com.example.contracts.events.PaymentCompletedEvent;
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
public class PaymentEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long timeoutMs;

    public PaymentEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.send-timeout-ms:200}") long timeoutMs
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.timeoutMs = timeoutMs;
    }

    public void publishPaymentCompleted(Long orderId, BigDecimal amount, BigDecimal expectedAmount, String status) {
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

        try {
            kafkaTemplate.send(PaymentKafkaTopics.PAYMENT_COMPLETED, String.valueOf(orderId), event)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            log.info("Published {} orderId={} status={} {}={}",
                    PaymentKafkaTopics.PAYMENT_COMPLETED, orderId, status,
                    CorrelationHeaders.CORRELATION_ID, correlationId);
        } catch (Exception ex) {
            log.warn("Failed to publish {} orderId={} status={} {}={} (non-fatal)",
                    PaymentKafkaTopics.PAYMENT_COMPLETED, orderId, status,
                    CorrelationHeaders.CORRELATION_ID, correlationId, ex);
        }
    }
}
