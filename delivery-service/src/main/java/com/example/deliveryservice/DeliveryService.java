package com.example.deliveryservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository repository;

    public DeliveryService(DeliveryRepository repository) {
        this.repository = repository;
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
            return repository.save(d);
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
