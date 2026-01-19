package com.example.orderservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private record CreateOrderRequest(Long userId, Long restaurantId) {}
    private record PayOrderRequest(Double amount) {}
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

    @GetMapping
    public List<OrderResponse> list() {
        return service.listAll().stream()
                .map(o -> new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(), o.getStatus()))
                .toList();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable Long id, @RequestBody PayOrderRequest req) {
        return service.pay(id, req.amount() != null ? req.amount() : 0.0)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(), o.getStatus())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }
}
