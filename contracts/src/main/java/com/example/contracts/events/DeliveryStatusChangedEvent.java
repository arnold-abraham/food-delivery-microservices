package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * MVP Kafka event.
 * Published by delivery-service when a delivery status changes.
 */
public record DeliveryStatusChangedEvent(
        @JsonProperty("eventVersion") int eventVersion,
        @JsonProperty("deliveryId") Long deliveryId,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("status") String status,
        @JsonProperty("changedAt") Instant changedAt,
        @JsonProperty("correlationId") String correlationId
) {
    public static final int VERSION = 1;
}

