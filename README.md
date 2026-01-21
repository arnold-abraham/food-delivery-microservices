# Food Delivery Microservices

A microservices system built with Java 17, Spring Boot 3.x, Eureka service discovery, and Spring Cloud Gateway.

## What’s included (base version)

- Service discovery via Eureka
- API gateway routing (single entrypoint)
- Postgres persistence for user/restaurant/order/delivery services (one Postgres container, separate DB per service)
- End-to-end flow:
  - create users (customer + driver)
  - create menu items
  - create order with items (total calculated from menu prices)
  - pay with exact amount (payment success/failure)
  - create delivery assignment with `driverId`
  - update delivery status (e.g., PICKED_UP, DELIVERED)
  - `GET /orders/{id}` includes latest delivery status
- Input validation with friendly 400 responses for invalid payloads

## Services

- **service-discovery**: Eureka server for service registry (http://localhost:8761)
- **api-gateway**: Spring Cloud Gateway as single entrypoint (http://localhost:8079)
- **user-service**: manages users (`/users/**`)
- **restaurant-service**: manages menu items (`/restaurants/{restaurantId}/menu/**`)
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

## End-to-end flow (via gateway)

```bash
# 1) Create a customer + a driver
curl -X POST http://localhost:8079/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'

curl -X POST http://localhost:8079/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Driver Dan","email":"driver@example.com"}'

# 2) Create menu items for restaurantId=1
curl -X POST http://localhost:8079/restaurants/1/menu \
  -H "Content-Type: application/json" \
  -d '{"name":"Margherita Pizza","price":10.0}'

curl -X POST http://localhost:8079/restaurants/1/menu \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta","price":12.0}'

# 3) Create an order with items (menuItemId from previous responses; example uses 1 and 2)
curl -X POST http://localhost:8079/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"restaurantId":1,"items":[{"menuItemId":1,"quantity":1},{"menuItemId":2,"quantity":1}]}'

# 4) Pay with exact amount (total should be 22.0 with the prices above) and assign driverId=2
curl -X POST http://localhost:8079/orders/1/pay \
  -H "Content-Type: application/json" \
  -d '{"amount":22.0,"driverId":2}'

# 5) Delivery lifecycle (lookup delivery id by order)
DELIVERY_ID=$(curl -s http://localhost:8079/deliveries/by-order/1 | python -c 'import sys,json; print(json.load(sys.stdin)["id"])')

curl -X PATCH http://localhost:8079/deliveries/$DELIVERY_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status":"PICKED_UP"}'

curl -X PATCH http://localhost:8079/deliveries/$DELIVERY_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}'

# 6) Order shows delivery status (and may auto-set status=DELIVERED)
curl http://localhost:8079/orders/1
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
