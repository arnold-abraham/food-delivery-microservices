package com.example.orderservice;

import com.example.orderservice.kafka.OrderEventsPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final OrderEventsPublisher eventsPublisher;

    @Value("${order.payment-service-url:http://payment-service:8084}")
    private String paymentServiceBaseUrl;

    @Value("${order.delivery-service-url:http://delivery-service:8086}")
    private String deliveryServiceBaseUrl;

    @Value("${order.restaurant-service-url:http://restaurant-service:8082}")
    private String restaurantServiceBaseUrl;

    @Value("${order.user-service-url:http://user-service:8081}")
    private String userServiceBaseUrl;

    private final Random random = new Random();

    public OrderService(OrderRepository repository,
                        OrderItemRepository orderItemRepository,
                        RestTemplate restTemplate,
                        OrderEventsPublisher eventsPublisher) {
        this.repository = repository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.eventsPublisher = eventsPublisher;
    }

    @Transactional
    public Order create(Long userId, Long restaurantId, List<CreateOrderItem> items) {
        Order order = repository.save(new Order(userId, restaurantId, "PENDING"));

        double total = 0.0;
        if (items != null) {
            for (CreateOrderItem it : items) {
                if (it.quantity() <= 0) {
                    throw new IllegalArgumentException("quantity must be >= 1");
                }

                // Fetch menu item from restaurant-service to get price + validate it belongs to restaurant
                String url = restaurantServiceBaseUrl + "/restaurants/" + restaurantId + "/menu/" + it.menuItemId();
                Map<?, ?> menuItem = restTemplate.getForObject(url, Map.class);
                if (menuItem == null || menuItem.get("price") == null) {
                    throw new IllegalArgumentException("Menu item not found: " + it.menuItemId());
                }

                double unitPrice = Double.parseDouble(String.valueOf(menuItem.get("price")));
                orderItemRepository.save(new OrderItem(order.getId(), it.menuItemId(), it.quantity(), unitPrice));
                total += unitPrice * it.quantity();
            }
        }

        order.setTotalAmount(total);
        Order saved = repository.save(order);

        eventsPublisher.publishOrderPlaced(saved.getId(), saved.getUserId(), saved.getRestaurantId());

        return saved;
    }

    @Transactional
    public Order create(Long userId, Long restaurantId) {
        return create(userId, restaurantId, List.of());
    }

    public Optional<Order> get(Long id) {
        return repository.findById(id);
    }

    public List<Order> listAll() {
        return repository.findAll();
    }

    public List<Order> listByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<OrderItem> getItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Transactional
    public Optional<Order> pay(Long orderId, double amount, Long driverId) {
        return repository.findById(orderId).map(order -> {
            double expectedAmount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;

            String paymentUrl = paymentServiceBaseUrl + "/payments";
            Map<String, Object> paymentRequest = Map.of(
                    "orderId", order.getId(),
                    "amount", amount,
                    "expectedAmount", expectedAmount
            );

            Map<?, ?> paymentResponse = restTemplate.postForObject(paymentUrl, paymentRequest, Map.class);
            boolean paymentSuccess = paymentResponse != null &&
                    "SUCCESS".equalsIgnoreCase(String.valueOf(paymentResponse.get("status")));

            if (!paymentSuccess) {
                order.setStatus("FAILED");
                return repository.save(order);
            }

            order.setStatus("PAID");
            Order saved = repository.save(order);

            long effectiveDriverId = driverId != null ? driverId : pickRandomDriver();
            eventsPublisher.publishOrderPaid(saved.getId(), effectiveDriverId);

            return saved;
        });
    }

    @Transactional
    public Optional<Order> syncDeliveryStatus(Long orderId) {
        return repository.findById(orderId).map(order -> {
            String url = deliveryServiceBaseUrl + "/deliveries/by-order/" + order.getId();
            try {
                Map<?, ?> delivery = restTemplate.getForObject(url, Map.class);
                if (delivery != null && delivery.get("status") != null) {
                    String status = String.valueOf(delivery.get("status"));
                    order.setDeliveryStatus(status);
                    if ("DELIVERED".equalsIgnoreCase(status)) {
                        order.setStatus("DELIVERED");
                    }
                    return repository.save(order);
                }
            } catch (RestClientException ex) {
                // best-effort: delivery service may be unavailable or no assignment yet
                return order;
            }
            return order;
        });
    }

    // Backwards compatible method used by older controller signature
    @Transactional
    public Optional<Order> pay(Long orderId, double amount) {
        return pay(orderId, amount, null);
    }

    private long pickRandomDriver() {
        try {
            String url = userServiceBaseUrl + "/users?role=DRIVER";
            List<Map<String, Object>> drivers = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (drivers != null && !drivers.isEmpty()) {
                Map<String, Object> picked = drivers.get(random.nextInt(drivers.size()));
                Object id = picked.get("id");
                if (id instanceof Number n) return n.longValue();
            }
        } catch (RestClientException ex) {
            // user-service unavailable — fall back to ID 1
        }
        return 1L;
    }

    public record CreateOrderItem(Long menuItemId, int quantity) {}
}
