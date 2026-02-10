package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Published when an order is placed (created).
 * Keep payload small and stable.
 */
public record OrderPlacedEvent(
        @JsonProperty("eventVersion") int eventVersion,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("restaurantId") Long restaurantId,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("correlationId") String correlationId
) {
    public static final int VERSION = 1;
}

