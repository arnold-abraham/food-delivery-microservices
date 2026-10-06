package com.example.deliveryservice.kafka;

import com.example.contracts.events.OrderPaidEvent;
import com.example.deliveryservice.Delivery;
import com.example.deliveryservice.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.*;

class OrderPaidListenerTest {

    private DeliveryService deliveryService;
    private OrderPaidListener listener;

    @BeforeEach
    void setUp() {
        deliveryService = mock(DeliveryService.class);
        listener = new OrderPaidListener(deliveryService);

        Delivery fakeDelivery = new Delivery(1L, 10L, "ASSIGNED");
        when(deliveryService.createAssignment(anyLong(), anyLong())).thenReturn(fakeDelivery);
    }

    @Test
    void callsCreateAssignmentOnValidEvent() {
        listener.onMessage(event(42L, 7L));

        verify(deliveryService).createAssignment(42L, 7L);
    }

    @Test
    void noOpOnNullEvent() {
        listener.onMessage(null);
        verifyNoInteractions(deliveryService);
    }

    @Test
    void noOpWhenOrderIdIsNull() {
        OrderPaidEvent event = new OrderPaidEvent(OrderPaidEvent.VERSION, null, 7L, Instant.now(), "corr");
        listener.onMessage(event);
        verifyNoInteractions(deliveryService);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private OrderPaidEvent event(Long orderId, Long driverId) {
        return new OrderPaidEvent(OrderPaidEvent.VERSION, orderId, driverId, Instant.now(), "corr-" + orderId);
    }
}
