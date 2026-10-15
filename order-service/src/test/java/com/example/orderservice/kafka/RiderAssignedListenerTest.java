package com.example.orderservice.kafka;

import com.example.contracts.events.RiderAssignedEvent;
import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiderAssignedListenerTest {

    private OrderRepository repo;
    private RiderAssignedListener listener;

    @BeforeEach
    void setUp() {
        repo = mock(OrderRepository.class);
        listener = new RiderAssignedListener(repo);
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void setsDeliveryStatusToAssigned() {
        Order order = orderWithDeliveryStatus(1L, null);
        when(repo.findById(1L)).thenReturn(Optional.of(order));

        listener.onMessage(event(1L, 10L));

        assertThat(order.getDeliveryStatus()).isEqualTo("ASSIGNED");
        verify(repo).save(order);
    }

    @Test
    void noOpWhenAlreadyAssigned() {
        Order order = orderWithDeliveryStatus(2L, "ASSIGNED");
        when(repo.findById(2L)).thenReturn(Optional.of(order));

        listener.onMessage(event(2L, 10L));

        verify(repo, never()).save(any());
    }

    @Test
    void noOpWhenOrderNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        listener.onMessage(event(99L, 10L));

        verify(repo, never()).save(any());
    }

    @Test
    void noOpOnNullEvent() {
        listener.onMessage(null);
        verifyNoInteractions(repo);
    }

    @Test
    void noOpWhenOrderIdIsZero() {
        listener.onMessage(eventWithOrderId(0L));
        verifyNoInteractions(repo);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Order orderWithDeliveryStatus(long id, String deliveryStatus) {
        Order order = new Order(1L, 1L, "PAID");
        try {
            var f = Order.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(order, id);
        } catch (Exception ignored) {}
        order.setDeliveryStatus(deliveryStatus);
        return order;
    }

    private RiderAssignedEvent event(long orderId, long driverId) {
        return new RiderAssignedEvent(RiderAssignedEvent.VERSION, orderId, 50L, driverId, Instant.now(), "corr-" + orderId);
    }

    private RiderAssignedEvent eventWithOrderId(long orderId) {
        return new RiderAssignedEvent(RiderAssignedEvent.VERSION, orderId, 50L, 10L, Instant.now(), null);
    }
}
