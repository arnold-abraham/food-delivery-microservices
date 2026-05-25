# Architecture Overview

## Request Flow

```
Browser / curl / Postman
        │
        │  HTTP (port 80 on EKS, 8079 local)
        ▼
┌─────────────────────────────────────────────────────┐
│  AWS ELB  (EKS)  │  localhost:8079  (Compose/k8s)  │
└───────────────────────────┬─────────────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │   api-gateway   │  JWT validation on every
                   │   port 8079     │  request (except /health,
                   └────────┬────────┘  /auth/register, /auth/login)
                            │
          ┌─────────────────┼──────────────────────┐
          │                 │                       │
          ▼                 ▼                       ▼
  ┌──────────────┐  ┌──────────────┐      ┌──────────────────┐
  │ user-service │  │  restaurant  │      │  order-service   │
  │  port 8081   │  │   -service   │      │   port 8083      │
  │  DB: userdb  │  │  port 8082   │      │  DB: orderdb     │
  └──────────────┘  │DB:restaurant │      └────────┬─────────┘
                    │     db       │               │
                    └──────────────┘    sync HTTP  │
                                        calls by   │
                                      order-service │
                              ┌────────────┬────────┴────────┐
                              │            │                  │
                              ▼            ▼                  ▼
                     ┌──────────────┐  ┌──────────┐  ┌────────────────┐
                     │   payment    │  │   user   │  │delivery-service│
                     │  -service    │  │ -service │  │   port 8086    │
                     │  port 8084   │  │(validate │  │ DB: deliverydb │
                     │              │  │ driver)  │  └────────────────┘
                     └──────────────┘  └──────────┘
```

## Async Events (Kafka — KRaft, no ZooKeeper)

```
order-service (publisher)                     Kafka topics
─────────────────────────────────────────     ─────────────────────────────
create order   ──────────────────────────►    order.placed.v1
pay() start    ──────────────────────────►    payment.requested.v1
pay() success  ──────────────────────────►    order.paid.v1
driver assigned ─────────────────────────►    delivery.rider.assigned.v1

delivery-service (publisher)
─────────────────────────────────────────
status update  ──────────────────────────►    delivery.status.changed.v1
                                                    │
                                                    └──► order-service (consumer)
                                                         updates order.deliveryStatus
```

## Data Stores

| Service             | Database       |
|---------------------|----------------|
| user-service        | userdb         |
| restaurant-service  | restaurantdb   |
| order-service       | orderdb        |
| delivery-service    | deliverydb     |
| payment-service     | *(stateless)*  |

All four databases live on a single PostgreSQL 15 instance.

## Deployment Modes

**Docker Compose (local)**
```
docker compose up --build
```
Services find each other by Compose service name (DNS). Prometheus + Grafana included.

**Minikube (local Kubernetes)**
```
eval $(minikube docker-env) && kubectl apply -f k8s/
```
Services find each other via Kubernetes Service DNS (`<name>.food.svc.cluster.local`).

**AWS EKS (cloud)**
```
bash scripts/eks-up.sh    # ~20 min, ~$0.28/hr
bash scripts/eks-down.sh  # destroys everything
```
Terraform provisions VPC, EKS cluster (m7i-flex.large nodes), EBS CSI driver, and ECR repos.  
Public access via AWS Classic Load Balancer on port 80.

## Auth

JWT (HS256) is validated at the gateway for every protected route.  
`JWT_SECRET` must be identical in `api-gateway` and `user-service` and at least 256 bits long.  
Generate one: `openssl rand -base64 48`