package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Published by order-service when payment succeeds.
 * Consumed by delivery-service to trigger rider assignment.
 */
public record OrderPaidEvent(
        @JsonProperty("eventVersion") int eventVersion,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("driverId") Long driverId,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("correlationId") String correlationId
) {
    public static final int VERSION = 1;
}
