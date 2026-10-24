package com.example.restaurantservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {
    public record CreateRestaurantRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "cuisine is required") String cuisine
    ) {}

    public record RestaurantResponse(Long id, String name, String cuisine) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    private final RestaurantService service;
    public RestaurantController(RestaurantService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateRestaurantRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        Restaurant r = service.create(req.name(), req.cuisine());
        return ResponseEntity.ok(new RestaurantResponse(r.getId(), r.getName(), r.getCuisine()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return service.get(id)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(new RestaurantResponse(r.getId(), r.getName(), r.getCuisine())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Restaurant not found")));
    }

    @GetMapping
    public PageResponse<RestaurantResponse> list(
            @RequestParam(name = "cuisine", required = false) String cuisine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size);
        Page<Restaurant> result = (cuisine == null || cuisine.isBlank())
                ? service.list(pageable)
                : service.listByCuisine(cuisine, pageable);
        List<RestaurantResponse> content = result.getContent().stream()
                .map(r -> new RestaurantResponse(r.getId(), r.getName(), r.getCuisine()))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
