package com.example.orderservice.kafka;

import com.example.contracts.events.RiderAssignedEvent;
import com.example.contracts.topics.DeliveryKafkaTopics;
import com.example.orderservice.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RiderAssignedListener {

    private static final Logger log = LoggerFactory.getLogger(RiderAssignedListener.class);

    private final OrderRepository orders;

    public RiderAssignedListener(OrderRepository orders) {
        this.orders = orders;
    }

    @KafkaListener(
            topics = DeliveryKafkaTopics.RIDER_ASSIGNED,
            properties = {"spring.json.value.default.type=com.example.contracts.events.RiderAssignedEvent"}
    )
    @Transactional
    public void onMessage(RiderAssignedEvent event) {
        if (event == null || event.orderId() == 0) {
            return;
        }

        orders.findById(event.orderId()).ifPresent(order -> {
            if ("ASSIGNED".equalsIgnoreCase(order.getDeliveryStatus())) {
                return; // idempotent no-op
            }
            order.setDeliveryStatus("ASSIGNED");
            orders.save(order);
            log.info("Order {} deliveryStatus set to ASSIGNED via delivery.rider.assigned.v1 (corrId={})",
                    order.getId(), event.correlationId());
        });
    }
}
