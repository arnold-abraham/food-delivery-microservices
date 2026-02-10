package com.example.contracts.topics;

/**
 * Kafka topics for delivery-related events.
 */
public final class DeliveryKafkaTopics {
    private DeliveryKafkaTopics() {}

    public static final String RIDER_ASSIGNED = "delivery.rider.assigned.v1";
    public static final String DELIVERY_STATUS_CHANGED = "delivery.status.changed.v1";
}

