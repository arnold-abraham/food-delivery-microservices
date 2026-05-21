# Food Delivery Microservices

Java 17 + Spring Boot microservices with API Gateway, Postgres, Kafka events, and JWT auth.

## What’s included

- API gateway routing (single entrypoint)
- DNS-based service discovery (Docker / Kubernetes)
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

## Run (Kubernetes / Minikube)

This repo includes a baseline Kubernetes setup in `k8s/`:

- **No Eureka**: services talk via Kubernetes DNS (`http://order-service:8083`, etc.) using `ClusterIP` Services.
- **Only the API Gateway is exposed**: via an Ingress.
- **Postgres and Kafka are StatefulSets** with PVCs for persistence.
- **Config is externalized** via ConfigMaps and Secrets.

### Prereqs

- `minikube`, `kubectl`
- An Ingress controller (on Minikube, the built-in addon works)

### Build images for Minikube

If your Minikube uses its own Docker daemon, you’ll typically do:

```bash
eval $(minikube -p minikube docker-env)
```

Then build the service images (tags match the manifests):

```bash
docker build -t api-gateway:0.0.1-SNAPSHOT ./api-gateway
docker build -t user-service:0.0.1-SNAPSHOT ./user-service
docker build -t restaurant-service:0.0.1-SNAPSHOT ./restaurant-service
docker build -t order-service:0.0.1-SNAPSHOT ./order-service
docker build -t payment-service:0.0.1-SNAPSHOT ./payment-service
docker build -t delivery-service:0.0.1-SNAPSHOT ./delivery-service
```

### Deploy

```bash
kubectl apply -f k8s/
```

Enable ingress on Minikube:

```bash
minikube addons enable ingress
```

Then access the Gateway through the Minikube ingress IP (or `minikube tunnel` depending on driver):

```bash
minikube ip
```

### Optional: Autoscaling

`k8s/40-order-hpa.yaml` includes an HPA for `order-service` (requires metrics-server in your cluster).

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
