package com.example.orderservice.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxRelayTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxRepository, kafkaTemplate, 200);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void relaysSinglePendingEvent() {
        OutboxEvent event = pendingEvent(1L, "order.placed.v1", "1", "{\"orderId\":1}");
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of(event));

        relay.relay();

        verify(kafkaTemplate).send("order.placed.v1", "1", "{\"orderId\":1}");
        verify(outboxRepository).markSent(eq(1L), any(Instant.class));
    }

    @Test
    void relaysMultiplePendingEvents() {
        OutboxEvent e1 = pendingEvent(1L, "order.placed.v1", "1", "{\"orderId\":1}");
        OutboxEvent e2 = pendingEvent(2L, "order.paid.v1", "2", "{\"orderId\":2}");
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of(e1, e2));

        relay.relay();

        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
        verify(outboxRepository).markSent(eq(1L), any(Instant.class));
        verify(outboxRepository).markSent(eq(2L), any(Instant.class));
    }

    @Test
    void skipsMarkSentWhenKafkaFails() {
        OutboxEvent event = pendingEvent(3L, "order.placed.v1", "3", "{\"orderId\":3}");
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        relay.relay();

        verify(outboxRepository, never()).markSent(anyLong(), any());
    }

    @Test
    void continuesAfterOneEventFails() {
        OutboxEvent failing = pendingEvent(1L, "order.placed.v1", "1", "{\"orderId\":1}");
        OutboxEvent ok      = pendingEvent(2L, "order.paid.v1",   "2", "{\"orderId\":2}");
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of(failing, ok));
        when(kafkaTemplate.send(eq("order.placed.v1"), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(eq("order.paid.v1"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        relay.relay();

        verify(outboxRepository, never()).markSent(eq(1L), any());
        verify(outboxRepository).markSent(eq(2L), any(Instant.class));
    }

    @Test
    void doesNothingWhenNoPendingEvents() {
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of());

        relay.relay();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).markSent(anyLong(), any());
    }

    @Test
    void markSentTimestampIsRecent() {
        OutboxEvent event = pendingEvent(1L, "order.placed.v1", "1", "{}");
        when(outboxRepository.findBySentAtIsNull()).thenReturn(List.of(event));

        Instant before = Instant.now();
        relay.relay();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> tsCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(outboxRepository).markSent(eq(1L), tsCaptor.capture());
        assertThat(tsCaptor.getValue()).isBetween(before, after);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private OutboxEvent pendingEvent(long id, String topic, String key, String payload) {
        OutboxEvent event = new OutboxEvent(topic, key, payload);
        try {
            var f = OutboxEvent.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(event, id);
        } catch (Exception ignored) {}
        return event;
    }
}
