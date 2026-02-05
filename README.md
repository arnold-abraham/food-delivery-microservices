# Food Delivery Microservices

A microservices system built with Java 17, Spring Boot 3.x, Eureka service discovery, and Spring Cloud Gateway.

## What’s included (base version)

- Service discovery via Eureka
- API gateway routing (single entrypoint)
- Postgres persistence for user/restaurant/order/delivery services (one Postgres container, separate DB per service)
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
- **restaurant-service**: manages menu items (`/restaurants/**`)
- **order-service**: orders + pay workflow (`/orders/**`)
- **payment-service**: validates payments (`/payments/**`)
- **delivery-service**: delivery assignments + status updates (`/deliveries/**`)

## Run locally (Docker)

```bash
mvn -DskipTests clean package
docker compose up --build
```

Open:
- Eureka dashboard: http://localhost:8761
- Gateway: http://localhost:8079

## Authentication (JWT)

Register + login to get a JWT, then pass it via `Authorization: Bearer <token>`.

```bash
curl -X POST http://localhost:8079/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"pass1234","roles":"CUSTOMER"}'

curl -X POST http://localhost:8079/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"pass1234"}'
```

## Demo flow

A runnable end-to-end flow script is included:

```bash
./scripts/demo-flow.sh
```

## Databases

A single Postgres container is started. Each DB-backed service uses its own database:
- user-service -> `userdb`
- restaurant-service -> `restaurantdb`
- order-service -> `orderdb`
- delivery-service -> `deliverydb`

The Postgres container creates these DBs on first start via `postgres/init.sql`.

### Connection details

- From inside Docker network:
  - host: `postgres`
  - port: `5432`
  - user: `food`
  - password: `food`

- From your host machine:
  - host: `localhost`
  - port: `5433` (mapped in `docker-compose.yml`)

Example:

```bash
psql "postgresql://food:food@localhost:5433/userdb"
```

## Architecture

See `diagrams/architecture.md`.
