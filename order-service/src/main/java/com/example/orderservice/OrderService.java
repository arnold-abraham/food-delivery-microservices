package com.example.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
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

    @Value("${order.delivery-service-url:http://delivery-service:8086}")
    private String deliveryServiceBaseUrl;

    @Value("${order.user-service-url:http://user-service:8081}")
    private String userServiceBaseUrl;

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
    public Optional<Order> pay(Long orderId, double amount, Long driverId) {
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
            Order saved = repository.save(order);

            if (paymentSuccess) {
                Long effectiveDriverId = (driverId != null ? driverId : 1L);

                // Validate driver exists in user-service (MVP: drivers are just users)
                String userUrl = userServiceBaseUrl + "/users/" + effectiveDriverId;
                try {
                    restTemplate.getForObject(userUrl, Map.class);
                } catch (HttpClientErrorException ex) {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        throw new IllegalArgumentException("driverId not found: " + effectiveDriverId);
                    }
                    throw ex;
                }

                // Create delivery assignment
                String deliveryUrl = deliveryServiceBaseUrl + "/deliveries";
                Map<String, Object> deliveryRequest = Map.of(
                        "orderId", saved.getId(),
                        "driverId", effectiveDriverId
                );
                restTemplate.postForObject(deliveryUrl, deliveryRequest, Map.class);
            }

            return saved;
        });
    }

    // Backwards compatible method used by older controller signature
    @Transactional
    public Optional<Order> pay(Long orderId, double amount) {
        return pay(orderId, amount, null);
    }
}
