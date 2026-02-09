package com.example.orderservice.kafka;

/** Kafka topic names used by order-service. */
public final class OrderKafkaTopics {
    private OrderKafkaTopics() {}

    public static final String DELIVERY_STATUS_CHANGED = "delivery.status.changed.v1";
}

