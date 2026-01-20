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

    public List<Delivery> listAll() {
        return repository.findAll();
    }

    @Transactional
    public Delivery createAssignment(Long orderId, Long driverId) {
        // MVP: always create as ASSIGNED
        return repository.save(new Delivery(orderId, driverId, "ASSIGNED"));
    }

    @Transactional
    public Optional<Delivery> updateStatus(Long id, String status) {
        return repository.findById(id).map(d -> {
            d.setStatus(status);
            return repository.save(d);
        });
    }
}
