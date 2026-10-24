package com.example.restaurantservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RestaurantService {
    private final RestaurantRepository repository;
    public RestaurantService(RestaurantRepository repository) { this.repository = repository; }

    @Transactional
    public Restaurant create(String name, String cuisine) { return repository.save(new Restaurant(name, cuisine)); }
    public Optional<Restaurant> get(Long id) { return repository.findById(id); }
    public Page<Restaurant> list(Pageable pageable) { return repository.findAll(pageable); }
    public Page<Restaurant> listByCuisine(String cuisine, Pageable pageable) {
        return repository.findByCuisineIgnoreCase(cuisine, pageable);
    }
}
