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

    @Value("${order.payment-service-url:http://payment-service:8084}")
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
            // Call payment-service
            String paymentUrl = paymentServiceBaseUrl + "/payments";
            Map<String, Object> paymentRequest = Map.of(
                    "orderId", order.getId(),
                    "amount", amount
            );

            Map<?, ?> paymentResponse = restTemplate.postForObject(paymentUrl, paymentRequest, Map.class);
            boolean paymentSuccess = paymentResponse != null &&
                    "SUCCESS".equalsIgnoreCase(String.valueOf(paymentResponse.get("status")));

            String newStatus = paymentSuccess ? "PAID" : "FAILED";
            order.setStatus(newStatus);
            return repository.save(order);
        });
    }
}
