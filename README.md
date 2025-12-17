⚠️ Status: This project is currently a work in progress. Several services and features are still under development.

# Food Delivery Microservices

A demo microservices system built with Java 17 and Spring Boot.

## Services

- **service-discovery**: Eureka server for service registry (http://localhost:8761)
- **api-gateway**: Spring Cloud Gateway as single entrypoint (http://localhost:8079)
- **user-service**: manages customers and delivery partners (exposed via gateway: `/users/**`)
- **restaurant-service**: manages restaurants and their menus (`/restaurants/**`)
- **order-service**: handles order creation and status updates (`/orders/**`)
- **payment-service**: simulates payment processing (`/payments/**`)
- **notification-service**: sends notifications (`/notifications/**`)

## Run locally (Docker)

1) Build all services

```bash
mvn -DskipTests clean package
```

2) Start the stack

```bash
docker-compose up --build
```

3) Open UIs

- Eureka dashboard: http://localhost:8761
- Gateway base: http://localhost:8079

## Databases

A single Postgres container is started. Each DB-backed service uses its own database:
- user-service -> `userdb`
- restaurant-service -> `restaurantdb`
- order-service -> `orderdb`

The Postgres container creates these DBs on first start via `postgres/init.sql`.

Default credentials (override with env vars in compose or service env):
- host: `postgres` (inside compose network), `localhost` (from host)
- port: `5432`
- user: `food`
- password: `food`

### Connect to Postgres (from host)

```bash
psql "postgresql://food:food@localhost:5432/fooddb"
```

Replace `fooddb` with `userdb`, `restaurantdb`, or `orderdb`.

### Connect from inside the container

```bash
docker-compose exec postgres psql -U food -d userdb
```

## Notes
- Services discover each other via Eureka. Gateway routes to services by logical names (lb://...).
- JPA `ddl-auto=update` will create tables automatically at runtime in each service's DB.
- If you already have a Postgres running on host port 5432, stop it or change the mapped port in `docker-compose.yml`.
