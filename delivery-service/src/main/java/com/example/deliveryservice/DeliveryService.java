package com.example.deliveryservice;

import com.example.deliveryservice.http.CorrelationIdHolder;
import com.example.deliveryservice.kafka.DeliveryEventsPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository repository;
    private final DeliveryEventsPublisher eventsPublisher;

    public DeliveryService(DeliveryRepository repository, DeliveryEventsPublisher eventsPublisher) {
        this.repository = repository;
        this.eventsPublisher = eventsPublisher;
    }

    public Optional<Delivery> get(Long id) {
        return repository.findById(id);
    }

    public Optional<Delivery> getByOrderId(Long orderId) {
        return repository.findFirstByOrderId(orderId);
    }

    public List<Delivery> listAll() {
        return repository.findAll();
    }

    @Transactional
    public Delivery createAssignment(Long orderId, Long driverId) {
        // MVP: always create as ASSIGNED
        return repository.save(new Delivery(orderId, driverId, DeliveryStatus.ASSIGNED.name()));
    }

    @Transactional
    public Optional<Delivery> updateStatus(Long id, String status) {
        final DeliveryStatus next = DeliveryStatus.from(status);

        return repository.findById(id).map(d -> {
            DeliveryStatus current = DeliveryStatus.from(d.getStatus());
            if (!current.canTransitionTo(next)) {
                throw new IllegalStateException("Invalid delivery status transition: " + current + " -> " + next);
            }
            d.setStatus(next.name());
            Delivery saved = repository.save(d);

            // best-effort event publish (avoid failing user request if Kafka is temporarily down)
            try {
                eventsPublisher.publishStatusChanged(saved.getId(), saved.getOrderId(), saved.getStatus(), CorrelationIdHolder.currentOrNull());
            } catch (Exception ex) {
                // swallow; delivery status is already persisted
            }

            return saved;
        });
    }

    enum DeliveryStatus {
        ASSIGNED,
        PICKED_UP,
        DELIVERED;

        static DeliveryStatus from(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("status is required");
            }
            try {
                return DeliveryStatus.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status: " + value);
            }
        }

        boolean canTransitionTo(DeliveryStatus next) {
            if (this == next) return true;
            return switch (this) {
                case ASSIGNED -> next == PICKED_UP;
                case PICKED_UP -> next == DELIVERED;
                case DELIVERED -> false;
            };
        }
    }
}
