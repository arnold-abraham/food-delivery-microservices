package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Emitted when a delivery has been assigned to a driver.
 *
 * Note: In the current implementation, the assignment is created synchronously via delivery-service (HTTP),
 * and this event is published best-effort for downstream consumers/observability.
 */
public record RiderAssignedEvent(
        @JsonProperty("eventVersion") int eventVersion,
        @JsonProperty("orderId") long orderId,
        @JsonProperty("deliveryId") long deliveryId,
        @JsonProperty("driverId") long driverId,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("correlationId") String correlationId
) {
    public static final int VERSION = 1;

    public RiderAssignedEvent {
        if (eventVersion <= 0) {
            eventVersion = VERSION;
        }
    }
}

