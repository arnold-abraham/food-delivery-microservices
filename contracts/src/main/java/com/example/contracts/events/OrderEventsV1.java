package com.example.contracts.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Versioned event payloads (no Kafka yet). These are intended to be published later.
 */
public final class OrderEventsV1 {
    private OrderEventsV1() {}

    public record OrderCreatedV1(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("userId") long userId,
            @JsonProperty("restaurantId") long restaurantId,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        public OrderCreatedV1 {
            if (eventVersion <= 0) eventVersion = 1;
        }

        public static OrderCreatedV1 v1(long orderId, long userId, long restaurantId, BigDecimal totalAmount, Instant createdAt, String correlationId) {
            return new OrderCreatedV1(1, orderId, userId, restaurantId, totalAmount, createdAt, correlationId);
        }
    }

    public record OrderPaidV1(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("paymentStatus") String paymentStatus,
            @JsonProperty("paidAt") Instant paidAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        public OrderPaidV1 {
            if (eventVersion <= 0) eventVersion = 1;
        }

        public static OrderPaidV1 v1(long orderId, BigDecimal amount, String paymentStatus, Instant paidAt, String correlationId) {
            return new OrderPaidV1(1, orderId, amount, paymentStatus, paidAt, correlationId);
        }
    }

    public record DeliveryAssignedV1(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("deliveryId") long deliveryId,
            @JsonProperty("driverId") long driverId,
            @JsonProperty("assignedAt") Instant assignedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        public DeliveryAssignedV1 {
            if (eventVersion <= 0) eventVersion = 1;
        }

        public static DeliveryAssignedV1 v1(long orderId, long deliveryId, long driverId, Instant assignedAt, String correlationId) {
            return new DeliveryAssignedV1(1, orderId, deliveryId, driverId, assignedAt, correlationId);
        }
    }
}

