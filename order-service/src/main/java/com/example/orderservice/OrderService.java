package com.example.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final RestTemplate restTemplate;

    @Value("${PAYMENT_SERVICE_URL:http://payment-service:8084}")
    private String paymentServiceBaseUrl;

    public OrderService(OrderRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Order create(Long userId, Long restaurantId) {
        return repository.save(new Order(userId, restaurantId, "PENDING"));
    }

    public Optional<Order> get(Long id) { return repository.findById(id); }

    public List<Order> listAll() { return repository.findAll(); }

    @Transactional
    public Optional<Order> pay(Long orderId, double amount) {
        return repository.findById(orderId).map(order -> {
            String url = paymentServiceBaseUrl + "/payments";
            Map<String, Object> request = Map.of(
                    "orderId", order.getId(),
                    "amount", amount
            );

            Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);
            String status = response != null && "SUCCESS".equalsIgnoreCase(String.valueOf(response.get("status")))
                    ? "PAID" : "FAILED";

            order.setStatus(status);
            return repository.save(order);
        });
    }
}
