package com.example.contracts.topics;

/** Kafka topic names used for order-related events. */
public final class OrderKafkaTopics {
    private OrderKafkaTopics() {}

    /** Published when an order is created/placed. */
    public static final String ORDER_PLACED = "order.placed.v1";

    /** Backwards-compatible alias (older name). */
    public static final String ORDER_CREATED = ORDER_PLACED;

    public static final String ORDER_PAID = "order.paid.v1";
    public static final String DELIVERY_ASSIGNED = "delivery.assigned.v1";
}
