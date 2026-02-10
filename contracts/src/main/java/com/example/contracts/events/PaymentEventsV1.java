package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Versioned payment-related event payloads.
 */
public final class PaymentEventsV1 {
    private PaymentEventsV1() {}

    public record PaymentRequestedV1(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("expectedAmount") BigDecimal expectedAmount,
            @JsonProperty("requestedAt") Instant requestedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        public PaymentRequestedV1 {
            if (eventVersion <= 0) eventVersion = 1;
        }

        public static PaymentRequestedV1 v1(long orderId, BigDecimal amount, BigDecimal expectedAmount, Instant requestedAt, String correlationId) {
            return new PaymentRequestedV1(1, orderId, amount, expectedAmount, requestedAt, correlationId);
        }
    }

    public record PaymentCompletedV1(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("expectedAmount") BigDecimal expectedAmount,
            @JsonProperty("paymentStatus") String paymentStatus,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        public PaymentCompletedV1 {
            if (eventVersion <= 0) eventVersion = 1;
        }

        public static PaymentCompletedV1 v1(long orderId, BigDecimal amount, BigDecimal expectedAmount, String paymentStatus, Instant completedAt, String correlationId) {
            return new PaymentCompletedV1(1, orderId, amount, expectedAmount, paymentStatus, completedAt, correlationId);
        }
    }
}

