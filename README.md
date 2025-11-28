⚠️ Status: This project is currently a work in progress. Several services and features are still under development.

# Food Delivery Microservices

A demo microservices system built with Java 17 and Spring Boot.

## Services

- **service-discovery**: Eureka server for service registry
- **api-gateway**: Spring Cloud Gateway as single entrypoint
- **user-service**: manages customers and delivery partners
- **restaurant-service**: manages restaurants and their menus
- **order-service**: handles order creation and status updates
- **payment-service**: simulates payment processing
- **notification-service**: sends notifications for key events

This repository is intentionally structured as a mono-repo to showcase
a microservices architecture.
