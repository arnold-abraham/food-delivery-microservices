package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a payment attempt completes (success/failure).
 */
public record PaymentCompletedEvent(
        @JsonProperty("eventVersion") int eventVersion,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("expectedAmount") BigDecimal expectedAmount,
        @JsonProperty("status") String status,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("correlationId") String correlationId
) {
    public static final int VERSION = 1;
}

