package com.example.deliveryservice;

import com.example.deliveryservice.kafka.DeliveryEventsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryServiceTest {

    private DeliveryRepository repository;
    private DeliveryEventsPublisher publisher;
    private DeliveryService service;

    @BeforeEach
    void setUp() {
        repository = mock(DeliveryRepository.class);
        publisher = mock(DeliveryEventsPublisher.class);
        service = new DeliveryService(repository, publisher);
        when(repository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createAssignment_persistsAndReturnsDelivery() {
        when(repository.findFirstByOrderId(1L)).thenReturn(Optional.empty());

        Delivery result = service.createAssignment(1L, 10L);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getDriverId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("ASSIGNED");
        verify(repository).save(any(Delivery.class));
    }

    @Test
    void createAssignment_idempotent_returnsExistingWhenAlreadyAssigned() {
        Delivery existing = new Delivery(1L, 10L, "ASSIGNED");
        when(repository.findFirstByOrderId(1L)).thenReturn(Optional.of(existing));

        Delivery result = service.createAssignment(1L, 99L);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void createAssignment_publishesRiderAssignedEvent() {
        when(repository.findFirstByOrderId(2L)).thenReturn(Optional.empty());

        service.createAssignment(2L, 5L);

        verify(publisher).publishRiderAssigned(any(), eq(2L), eq(5L), any());
    }

    @Test
    void createAssignment_doesNotPublishOnDuplicate() {
        when(repository.findFirstByOrderId(3L)).thenReturn(Optional.of(new Delivery(3L, 5L, "ASSIGNED")));

        service.createAssignment(3L, 5L);

        verifyNoInteractions(publisher);
    }

    @Test
    void updateStatus_validTransition_updatesAndReturns() {
        Delivery delivery = new Delivery(1L, 10L, "ASSIGNED");
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        Optional<Delivery> result = service.updateStatus(1L, "PICKED_UP");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("PICKED_UP");
    }

    @Test
    void updateStatus_invalidTransition_throws() {
        Delivery delivery = new Delivery(1L, 10L, "DELIVERED");
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.updateStatus(1L, "PICKED_UP"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED -> PICKED_UP");
    }

    @Test
    void updateStatus_notFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.updateStatus(99L, "PICKED_UP")).isEmpty();
    }
}
