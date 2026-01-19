package com.example.orderservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    public record CreateOrderRequest(
            @NotNull(message = "userId is required") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @NotNull(message = "restaurantId is required") @Min(value = 1, message = "restaurantId must be >= 1") Long restaurantId
    ) {}

    public record PayOrderRequest(
            @NotNull(message = "amount is required") @Min(value = 1, message = "amount must be >= 1") Double amount
    ) {}

    public record OrderResponse(Long id, Long userId, Long restaurantId, String status) {}

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
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
    public ResponseEntity<?> pay(@PathVariable Long id, @Valid @RequestBody PayOrderRequest req) {
        return service.pay(id, req.amount())
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(new OrderResponse(o.getId(), o.getUserId(), o.getRestaurantId(), o.getStatus())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }
}
