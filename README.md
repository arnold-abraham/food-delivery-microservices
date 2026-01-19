# Food Delivery Microservices

A demo microservices system built with Java 17, Spring Boot 3.x, Eureka service discovery, and Spring Cloud Gateway.

## What’s included (base version)

- Service discovery via Eureka
- API gateway routing (single entrypoint)
- Postgres persistence for user/restaurant/order services (one Postgres container, separate DB per service)
- Order workflow:
  - create order (PENDING)
  - pay order -> status becomes PAID or FAILED
  - send a notification after payment attempt
- Input validation and consistent 400 responses for invalid payloads

## Services

- **service-discovery**: Eureka server for service registry (http://localhost:8761)
- **api-gateway**: Spring Cloud Gateway as single entrypoint (http://localhost:8079)
- **user-service**: manages customers (`/users/**`)
- **restaurant-service**: manages restaurants (`/restaurants/**`)
- **order-service**: handles orders + pay workflow (`/orders/**`)
- **payment-service**: simulates payments (`/payments/**`)
- **notification-service**: logs notifications (`/notifications/**`)

## Run locally (Docker)

```bash
mvn -DskipTests clean package
docker compose up --build
```

Open:
- Eureka dashboard: http://localhost:8761
- Gateway root: http://localhost:8079/

## Manual smoke test (via gateway)

```bash
# Create user
curl -X POST http://localhost:8079/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'

# Create restaurant
curl -X POST http://localhost:8079/restaurants \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta Place","cuisine":"Italian"}'

# Create order
curl -X POST http://localhost:8079/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"restaurantId":1}'

# Pay order (random SUCCESS/FAILED in payment-service)
curl -X POST http://localhost:8079/orders/1/pay \
  -H "Content-Type: application/json" \
  -d '{"amount":15.0}'

# Verify order status
curl http://localhost:8079/orders/1
```

To see notifications being produced:

```bash
docker compose logs -f notification-service
```

## Databases

A single Postgres container is started. Each DB-backed service uses its own database:
- user-service -> `userdb`
- restaurant-service -> `restaurantdb`
- order-service -> `orderdb`

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
