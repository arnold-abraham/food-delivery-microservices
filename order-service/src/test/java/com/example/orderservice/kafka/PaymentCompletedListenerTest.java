package com.example.orderservice.kafka;

import com.example.contracts.events.PaymentCompletedEvent;
import com.example.orderservice.Order;
import com.example.orderservice.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentCompletedListenerTest {

    private OrderRepository repo;
    private PaymentCompletedListener listener;

    @BeforeEach
    void setUp() {
        repo = mock(OrderRepository.class);
        listener = new PaymentCompletedListener(repo);
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void setsOrderToPaidWhenPaymentSucceeds() {
        Order order = pendingOrder(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(order));

        listener.onMessage(event(1L, "SUCCESS"));

        assertThat(order.getStatus()).isEqualTo("PAID");
        verify(repo).save(order);
    }

    @Test
    void setsOrderToFailedWhenPaymentFails() {
        Order order = pendingOrder(2L);
        when(repo.findById(2L)).thenReturn(Optional.of(order));

        listener.onMessage(event(2L, "FAILED"));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        verify(repo).save(order);
    }

    @Test
    void noOpWhenOrderAlreadyPaid() {
        Order order = orderWithStatus(3L, "PAID");
        when(repo.findById(3L)).thenReturn(Optional.of(order));

        listener.onMessage(event(3L, "SUCCESS"));

        assertThat(order.getStatus()).isEqualTo("PAID");
        verify(repo, never()).save(any());
    }

    @Test
    void noOpWhenOrderAlreadyFailed() {
        Order order = orderWithStatus(4L, "FAILED");
        when(repo.findById(4L)).thenReturn(Optional.of(order));

        listener.onMessage(event(4L, "FAILED"));

        verify(repo, never()).save(any());
    }

    @Test
    void noOpWhenOrderIsDelivered() {
        Order order = orderWithStatus(5L, "DELIVERED");
        when(repo.findById(5L)).thenReturn(Optional.of(order));

        listener.onMessage(event(5L, "SUCCESS"));

        verify(repo, never()).save(any());
    }

    @Test
    void noOpOnNullEvent() {
        listener.onMessage(null);
        verifyNoInteractions(repo);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Order pendingOrder(long id) {
        return orderWithStatus(id, "PENDING");
    }

    private Order orderWithStatus(long id, String status) {
        Order order = new Order(1L, 1L, status);
        try {
            var f = Order.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(order, id);
        } catch (Exception ignored) {}
        return order;
    }

    private PaymentCompletedEvent event(long orderId, String status) {
        return new PaymentCompletedEvent(
                PaymentCompletedEvent.VERSION,
                orderId,
                BigDecimal.TEN,
                BigDecimal.TEN,
                status,
                Instant.now(),
                "corr-" + orderId
        );
    }
}
