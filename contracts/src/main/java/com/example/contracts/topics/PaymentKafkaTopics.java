package com.example.contracts.topics;

/** Kafka topic names used for payment-related events. */
public final class PaymentKafkaTopics {
    private PaymentKafkaTopics() {}

    public static final String PAYMENT_REQUESTED = "payment.requested.v1";
    public static final String PAYMENT_COMPLETED = "payment.completed.v1";
}

