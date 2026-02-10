package com.example.orderservice.kafka;

import com.example.contracts.events.DeliveryStatusChangedEvent;
import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeliveryStatusChangedListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStatusChangedListener.class);

    private final OrderRepository orders;

    public DeliveryStatusChangedListener(OrderRepository orders) {
        this.orders = orders;
    }

    @KafkaListener(topics = OrderKafkaTopics.DELIVERY_STATUS_CHANGED)
    @Transactional
    public void onMessage(DeliveryStatusChangedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }

        orders.findById(event.orderId()).ifPresent(order -> apply(order, event));
    }

    private void apply(Order order, DeliveryStatusChangedEvent event) {
        String next = event.status();
        if (next == null || next.isBlank()) {
            return;
        }

        String current = order.getDeliveryStatus();
        if (next.equalsIgnoreCase(current)) {
            // idempotent no-op
            return;
        }

        order.setDeliveryStatus(next.toUpperCase());

        if ("DELIVERED".equalsIgnoreCase(next)) {
            order.setStatus("DELIVERED");
        }

        orders.save(order);
        log.info("Order {} deliveryStatus updated to {} (corrId={})", order.getId(), next, event.correlationId());
    }
}

