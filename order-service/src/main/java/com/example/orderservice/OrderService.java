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

    @Value("${order.notification-service-url:http://notification-service:8085}")
    private String notificationServiceBaseUrl;

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
            // 1) Call payment-service
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
            Order saved = repository.save(order);

            // 2) Best-effort notification
            try {
                String notificationUrl = notificationServiceBaseUrl + "/notifications";
                String message = paymentSuccess
                        ? "Order %d payment successful".formatted(saved.getId())
                        : "Order %d payment failed".formatted(saved.getId());

                Map<String, Object> notificationRequest = Map.of(
                        "orderId", saved.getId(),
                        "message", message
                );

                restTemplate.postForObject(notificationUrl, notificationRequest, Map.class);
            } catch (Exception ignored) {
                // Do not fail the payment flow if notification fails
            }

            return saved;
        });
    }
}
