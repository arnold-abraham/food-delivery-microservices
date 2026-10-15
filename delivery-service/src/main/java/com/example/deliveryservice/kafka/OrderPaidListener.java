package com.example.deliveryservice.kafka;

import com.example.contracts.events.OrderPaidEvent;
import com.example.contracts.topics.OrderKafkaTopics;
import com.example.deliveryservice.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidListener.class);

    private final DeliveryService deliveryService;

    public OrderPaidListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = OrderKafkaTopics.ORDER_PAID)
    public void onMessage(OrderPaidEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }

        log.info("Received order.paid.v1 orderId={} driverId={} (corrId={})",
                event.orderId(), event.driverId(), event.correlationId());

        deliveryService.createAssignment(event.orderId(), event.driverId());
    }
}
