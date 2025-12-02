package com.example.orderservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) { this.repository = repository; }

    @Transactional
    public Order create(Long userId, Long restaurantId) { return repository.save(new Order(userId, restaurantId, "PENDING")); }
    public Optional<Order> get(Long id) { return repository.findById(id); }
}

