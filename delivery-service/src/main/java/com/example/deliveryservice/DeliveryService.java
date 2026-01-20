package com.example.deliveryservice;

import org.springframework.stereotype.Service;

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
}

