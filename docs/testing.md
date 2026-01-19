# Testing

## Automated tests (Maven)

Run unit/controller tests for all modules:

```bash
mvn test
```

These tests are intentionally lightweight:
- `@WebMvcTest` controller tests validate request validation and error responses
- a small smoke test for payment controller

## Manual end-to-end smoke test (Docker)

Start the stack:

```bash
mvn -DskipTests clean package
docker compose up --build
```

Verify services are registered in Eureka:
- http://localhost:8761

Then run the gateway smoke test:

```bash
curl -X POST http://localhost:8079/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'

curl -X POST http://localhost:8079/restaurants \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta Place","cuisine":"Italian"}'

curl -X POST http://localhost:8079/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"restaurantId":1}'

curl -X POST http://localhost:8079/orders/1/pay \
  -H "Content-Type: application/json" \
  -d '{"amount":15.0}'

curl http://localhost:8079/orders/1
```

Check notifications:

```bash
docker compose logs -f notification-service
```

