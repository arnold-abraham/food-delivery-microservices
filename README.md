# Food Delivery Microservices

Java 17 + Spring Boot microservices with Eureka, Spring Cloud Gateway, Postgres, Kafka events, and JWT auth.

## What’s included

- Service discovery via Eureka
- API gateway routing (single entrypoint)
- Postgres persistence for user/restaurant/order/delivery services (one Postgres container, separate DB per service)
- Kafka events for key lifecycle updates (order/payment/delivery)
- End-to-end flow:
  - user registration + JWT login
  - create menu items
  - create order with items (total calculated from menu prices)
  - pay with exact amount
  - assign delivery to a driver and update delivery status
  - `GET /orders/{id}` includes latest delivery status

## Services

- **service-discovery**: Eureka server (http://localhost:8761)
- **api-gateway**: single entrypoint (http://localhost:8079)
- **user-service**: users + auth (`/users/**`, `/auth/**`)
- **restaurant-service**: restaurants + menu (`/restaurants/**`)
- **order-service**: orders + pay (`/orders/**`)
- **payment-service**: payments (`/payments/**`)
- **delivery-service**: deliveries (`/deliveries/**`)

## Run (Docker)

```bash
mvn -DskipTests clean package
docker compose up --build
```

## Auth (JWT)

Most endpoints are protected. Obtain a token via `/auth/register` and `/auth/login`, then pass:

- `Authorization: Bearer <token>`

## Kafka

Kafka runs via Docker Compose. See `scripts/kafka-sanity.sh` for a quick local topic/consumer demo.

## Observability

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Architecture

See `diagrams/architecture.md`.
