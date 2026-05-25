# Food Delivery Microservices

Java 17 + Spring Boot 3.2, API Gateway, PostgreSQL, Kafka (KRaft), JWT auth.

## Services

| Service | Port | Routes |
|---|---|---|
| api-gateway | 8079 | all traffic — single entrypoint |
| user-service | 8081 | `/auth/**`, `/users/**` |
| restaurant-service | 8082 | `/restaurants/**` |
| order-service | 8083 | `/orders/**` |
| payment-service | 8084 | `/payments/**` |
| delivery-service | 8086 | `/deliveries/**` |

## Run — Docker Compose

```bash
cp .env.example .env        # fill in JWT_SECRET (openssl rand -base64 48)
mvn -DskipTests clean package
docker compose up --build
```

Gateway: `http://localhost:8079`

## Run — Minikube

```bash
mvn -DskipTests clean package
eval $(minikube docker-env)

for svc in api-gateway user-service restaurant-service order-service payment-service delivery-service; do
  docker build -t ${svc}:0.0.1-SNAPSHOT ./${svc}/
done

# fill in JWT_SECRET in k8s/02-secrets.yaml before applying
kubectl apply -f k8s/
kubectl port-forward -n food svc/api-gateway 8079:8079
```

## Run — AWS EKS

One-command bring-up (~20 min first run):

```bash
bash scripts/eks-up.sh
```

Prints a public Load Balancer URL when ready. Tear down everything when done:

```bash
bash scripts/eks-down.sh
```

**Prereqs:** `terraform`, `aws` CLI configured, `kubectl`, Docker.  
**Cost:** ~$0.28/hour while running (2 × m7i-flex.large + EKS control plane + NAT gateway).

## End-to-end test

```bash
# Docker / Minikube (port-forwarded)
bash scripts/demo-flow.sh

# EKS (use the Load Balancer URL printed by eks-up.sh)
GATEWAY_URL=http://<lb-hostname> bash scripts/demo-flow.sh
```

Flow: register → login → create restaurant + menu → place order → pay → assign driver → deliver.

## Auth

All endpoints except `/health`, `/auth/register`, `/auth/login` require:

```
Authorization: Bearer <token>
```

Get a token via `POST /auth/login`.

## Secrets

`k8s/02-secrets.yaml` and `.env.example` contain **placeholders only** — never commit real secrets.  
Generate a JWT secret: `openssl rand -base64 48`

## Observability

Prometheus and Grafana are included in Docker Compose (`localhost:9090` / `localhost:3000`).  
All services expose `/actuator/health` and `/actuator/prometheus`.

## Architecture

See `diagrams/architecture.md`.