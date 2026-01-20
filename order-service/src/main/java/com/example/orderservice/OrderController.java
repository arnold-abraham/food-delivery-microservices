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

    public record CreateOrderItemRequest(
            @NotNull(message = "menuItemId is required") @Min(value = 1, message = "menuItemId must be >= 1") Long menuItemId,
            @Min(value = 1, message = "quantity must be >= 1") int quantity
    ) {}

    public record CreateOrderRequest(
            @NotNull(message = "userId is required") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @NotNull(message = "restaurantId is required") @Min(value = 1, message = "restaurantId must be >= 1") Long restaurantId,
            List<CreateOrderItemRequest> items
    ) {}

    public record PayOrderRequest(
            @NotNull(message = "amount is required") @Min(value = 1, message = "amount must be >= 1") Double amount,
            @Min(value = 1, message = "driverId must be >= 1") Long driverId
    ) {}

    public record OrderItemResponse(Long menuItemId, int quantity, double unitPrice, double lineTotal) {}

    public record OrderResponse(Long id, Long userId, Long restaurantId, String status, Double totalAmount, String deliveryStatus, List<OrderItemResponse> items) {}

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        List<OrderService.CreateOrderItem> items = req.items() == null ? List.of() : req.items().stream()
                .map(i -> new OrderService.CreateOrderItem(i.menuItemId(), i.quantity()))
                .toList();

        Order o = service.create(req.userId(), req.restaurantId(), items);
        return ResponseEntity.ok(toResponse(o));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        // best-effort: refresh delivery status each time
        service.syncDeliveryStatus(id);

        return service.get(id)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(toResponse(o)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }

    @PostMapping("/{id}/refresh-delivery")
    public ResponseEntity<?> refreshDelivery(@PathVariable Long id) {
        return service.syncDeliveryStatus(id)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(toResponse(o)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }

    @GetMapping
    public List<OrderResponse> list() {
        return service.listAll().stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable Long id, @Valid @RequestBody PayOrderRequest req) {
        return service.pay(id, req.amount(), req.driverId())
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(toResponse(o)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = service.getItems(o.getId()).stream()
                .map(oi -> new OrderItemResponse(oi.getMenuItemId(), oi.getQuantity(), oi.getUnitPrice(), oi.lineTotal()))
                .toList();

        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getRestaurantId(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getDeliveryStatus(),
                items
        );
    }
}
