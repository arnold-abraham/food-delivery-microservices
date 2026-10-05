package com.example.orderservice.kafka;

import com.example.contracts.events.PaymentCompletedEvent;
import com.example.contracts.topics.PaymentKafkaTopics;
import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedListener.class);

    private final OrderRepository orders;

    public PaymentCompletedListener(OrderRepository orders) {
        this.orders = orders;
    }

    @KafkaListener(
            topics = PaymentKafkaTopics.PAYMENT_COMPLETED,
            properties = {"spring.json.value.default.type=com.example.contracts.events.PaymentCompletedEvent"}
    )
    @Transactional
    public void onMessage(PaymentCompletedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        orders.findById(event.orderId()).ifPresent(order -> apply(order, event));
    }

    private void apply(Order order, PaymentCompletedEvent event) {
        // Reconciliation path: the sync HTTP flow already sets the status correctly.
        // Only act if the order is still PENDING — covers cases where the HTTP response
        // was lost or the payment call was never made.
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        String next = "SUCCESS".equalsIgnoreCase(event.status()) ? "PAID" : "FAILED";
        order.setStatus(next);
        orders.save(order);
        log.info("Order {} status reconciled to {} via payment.completed.v1 (corrId={})",
                order.getId(), next, event.correlationId());
    }
}
