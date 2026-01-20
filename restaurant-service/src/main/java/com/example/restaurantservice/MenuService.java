package com.example.restaurantservice;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public MenuItem create(Long restaurantId, String name, double price) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new IllegalArgumentException("Restaurant not found: " + restaurantId);
        }
        return menuItemRepository.save(new MenuItem(restaurantId, name, price));
    }

    public List<MenuItem> listByRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    public Optional<MenuItem> get(Long id) {
        return menuItemRepository.findById(id);
    }
}

