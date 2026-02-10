# Food Delivery Microservices

Java 17 + Spring Boot microservices with Eureka, Spring Cloud Gateway, Postgres, and Kafka event publishing.

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

- **service-discovery**: Eureka server for service registry (http://localhost:8761)
- **api-gateway**: Spring Cloud Gateway as single entrypoint (http://localhost:8079)
- **user-service**: manages users (`/users/**`, `/auth/**`)
- **restaurant-service**: manages restaurants + menu items (`/restaurants/**`)
- **order-service**: orders + pay workflow (`/orders/**`)
- **payment-service**: validates payments (`/payments/**`)
- **delivery-service**: delivery assignments + status updates (`/deliveries/**`)

## Run (Docker)

```bash
mvn -DskipTests clean package
docker compose up --build
```

## Auth (JWT)

Register + login, then call APIs with `Authorization: Bearer <token>`.

```bash
curl -X POST http://localhost:8079/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"pass1234","roles":"CUSTOMER"}'

curl -X POST http://localhost:8079/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"pass1234"}'
```

## Architecture

See `diagrams/architecture.md`.
