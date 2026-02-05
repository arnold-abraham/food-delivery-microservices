package com.example.deliveryservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    public record CreateDeliveryRequest(
            @NotNull(message = "orderId is required") @Min(value = 1, message = "orderId must be >= 1") Long orderId,
            @NotNull(message = "driverId is required") @Min(value = 1, message = "driverId must be >= 1") Long driverId
    ) {}

    public record UpdateDeliveryStatusRequest(
            @NotBlank(message = "status is required") String status
    ) {}

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Delivery> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Delivery> get(@PathVariable Long id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<?> getByOrderId(@PathVariable Long orderId) {
        return service.getByOrderId(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Delivery not found")));
    }

    @PostMapping
    public ResponseEntity<Delivery> create(@Valid @RequestBody CreateDeliveryRequest req) {
        Delivery created = service.createAssignment(req.orderId(), req.driverId());
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateDeliveryStatusRequest req) {
        try {
            return service.updateStatus(id, req.status())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Delivery not found")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            // invalid transition
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }
    }
}
