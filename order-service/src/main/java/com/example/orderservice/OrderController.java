package com.example.orderservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private record CreateOrderRequest(Long userId, Long restaurantId) {}
    private record OrderResponse(Long id, Long userId, Long restaurantId, String status) {}

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest req) {
        Order o = service.create(req.userId(), req.restaurantId());
        return ResponseEntity.ok(new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(), o.getStatus()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return service.get(id)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(), o.getStatus())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }
}

