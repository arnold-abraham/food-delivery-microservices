package com.example.orderservice.kafka;

import com.example.contracts.events.DeliveryStatusChangedEvent;
import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryStatusChangedListenerTest {

    @Test
    void updatesOrderDeliveryStatusAndStatusWhenDelivered() {
        OrderRepository repo = mock(OrderRepository.class);
        DeliveryStatusChangedListener listener = new DeliveryStatusChangedListener(repo);

        Order order = new Order(1L, 1L, "PAID");
        // simulate persisted id
        try {
            var f = Order.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(order, 10L);
        } catch (Exception ignored) {}

        when(repo.findById(10L)).thenReturn(Optional.of(order));
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                1,
                5L,
                10L,
                "DELIVERED",
                Instant.now(),
                "corr-1"
        );

        listener.onMessage(event);

        assertThat(order.getDeliveryStatus()).isEqualTo("DELIVERED");
        assertThat(order.getStatus()).isEqualTo("DELIVERED");
        verify(repo).save(order);
    }
}

