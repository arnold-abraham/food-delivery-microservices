package com.example.deliveryservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<Delivery> list(
            @RequestParam(required = false) Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size);
        Page<Delivery> result = driverId != null
                ? service.listByDriverId(driverId, pageable)
                : service.listAll(pageable);
        return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
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
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDeliveryRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        Delivery created = service.createAssignment(req.orderId(), req.driverId());
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliveryStatusRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return service.get(id)
                .<ResponseEntity<?>>map(delivery -> {
                    boolean isAdmin = roles != null && roles.contains("ADMIN");
                    boolean isAssignedDriver = delivery.getDriverId() != null
                            && delivery.getDriverId().equals(callerId);
                    if (!isAdmin && !isAssignedDriver) {
                        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
                    }
                    try {
                        return service.updateStatus(id, req.status())
                                .<ResponseEntity<?>>map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Delivery not found")));
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
                    } catch (IllegalStateException ex) {
                        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Delivery not found")));
    }
}
