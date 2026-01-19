package com.example.restaurantservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    private final RestaurantService service;
    public RestaurantController(RestaurantService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody CreateRestaurantRequest req) {
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
    public List<RestaurantResponse> list() {
        return service.list().stream().map(r -> new RestaurantResponse(r.getId(), r.getName(), r.getCuisine())).toList();
    }
}
