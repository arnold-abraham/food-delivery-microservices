package com.example.restaurantservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menu")
public class MenuController {

    public record CreateMenuItemRequest(
            @NotBlank(message = "name is required") String name,
            @NotNull(message = "price is required") @Min(value = 1, message = "price must be >= 1") Double price
    ) {}

    public record MenuItemResponse(Long id, Long restaurantId, String name, double price) {}

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> create(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest req
    ) {
        MenuItem created = service.create(restaurantId, req.name(), req.price());
        return ResponseEntity.ok(new MenuItemResponse(created.getId(), created.getRestaurantId(), created.getName(), created.getPrice()));
    }

    @GetMapping
    public List<MenuItemResponse> list(@PathVariable Long restaurantId) {
        return service.listByRestaurant(restaurantId).stream()
                .map(mi -> new MenuItemResponse(mi.getId(), mi.getRestaurantId(), mi.getName(), mi.getPrice()))
                .toList();
    }

    @GetMapping("/{menuItemId}")
    public ResponseEntity<?> get(@PathVariable Long restaurantId, @PathVariable Long menuItemId) {
        return service.get(menuItemId)
                .<ResponseEntity<?>>map(mi -> {
                    if (!restaurantId.equals(mi.getRestaurantId())) {
                        return ResponseEntity.status(404).body(Map.of("error", "Menu item not found"));
                    }
                    return ResponseEntity.ok(new MenuItemResponse(mi.getId(), mi.getRestaurantId(), mi.getName(), mi.getPrice()));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Menu item not found")));
    }
}

