# Architecture Overview

```mermaid
flowchart LR
    client["Clients<br/>Browser · curl · Postman"]
    ingress["LoadBalancer Service / AWS ELB<br/>EKS :80"]
    gateway["API Gateway<br/>:8079<br/><br/>JWT validation"]

    subgraph services["HTTP Services"]
        direction TB
        user["user-service<br/>:8081<br/>/auth · /users"]
        restaurant["restaurant-service<br/>:8082<br/>/restaurants"]
        order["order-service<br/>:8083<br/>/orders"]
        payment["payment-service<br/>:8084<br/>/payments<br/><br/>stateless"]
        delivery["delivery-service<br/>:8086<br/>/deliveries"]
    end

    subgraph data["Data Layer"]
        postgres[("PostgreSQL 15<br/>:5432")]
        userdb[("userdb")]
        restaurantdb[("restaurantdb")]
        orderdb[("orderdb")]
        outbox[("outbox_events<br/>order-service")]
        deliverydb[("deliverydb")]
    end

    subgraph events["Async Events · Kafka KRaft"]
        kafka[("Kafka broker<br/>:9092 internal<br/>:19092 host")]
        placed["order.placed.v1"]
        requested["payment.requested.v1"]
        completed["payment.completed.v1"]
        assigned["delivery.rider.assigned.v1"]
        status["delivery.status.changed.v1"]
        paid["order.paid.v1"]
    end

    subgraph observability["Observability"]
        prometheus["Prometheus<br/>:9090"]
        grafana["Grafana<br/>:3000"]
    end

    client -->|"HTTP"| ingress
    client -->|"HTTP :8079 local"| gateway
    ingress --> gateway

    gateway -->|"/auth/** · /users/**"| user
    gateway -->|"/restaurants/**"| restaurant
    gateway -->|"/orders/**"| order
    gateway -->|"/payments/**"| payment
    gateway -->|"/deliveries/**"| delivery

    order -->|"GET menu item<br/>sync HTTP"| restaurant
    order -->|"POST payment<br/>sync HTTP"| payment
    order -->|"GET delivery<br/>sync HTTP"| delivery

    user --- userdb
    restaurant --- restaurantdb
    order --- orderdb
    order -->|"write<br/>(same tx)"| outbox
    outbox --- orderdb
    delivery --- deliverydb
    userdb --- postgres
    restaurantdb --- postgres
    orderdb --- postgres
    deliverydb --- postgres

    outbox -.->|"relay"| placed
    outbox -.->|"relay"| paid
    payment -.->|"publish"| requested
    payment -.->|"publish"| completed
    delivery -.->|"publish on assignment"| assigned
    delivery -.->|"publish on status update"| status

    placed --> kafka
    paid --> kafka
    requested --> kafka
    completed --> kafka
    assigned --> kafka
    status --> kafka
    kafka -.->|"consume"| orderPaidListener["delivery-service<br/>order.paid listener"]
    orderPaidListener -->|"create assignment"| delivery
    kafka -.->|"consume"| riderAssignedListener["order-service<br/>rider.assigned listener"]
    riderAssignedListener -->|"update deliveryStatus ASSIGNED"| order
    kafka -.->|"consume"| statusListener["order-service<br/>delivery listener"]
    statusListener -->|"update deliveryStatus<br/>DELIVERED → order status"| order
    kafka -.->|"consume"| paymentListener["order-service<br/>payment listener"]
    paymentListener -->|"reconcile payment status<br/>PENDING → PAID / FAILED"| order

    prometheus -.->|"scrapes /actuator/prometheus"| gateway
    prometheus -.-> user
    prometheus -.-> restaurant
    prometheus -.-> order
    prometheus -.-> payment
    prometheus -.-> delivery
    grafana -->|"queries"| prometheus

    classDef edge fill:#172554,stroke:#60a5fa,color:#fff,stroke-width:2px
    classDef service fill:#064e3b,stroke:#34d399,color:#fff,stroke-width:2px
    classDef database fill:#581c87,stroke:#c084fc,color:#fff,stroke-width:2px
    classDef broker fill:#312e81,stroke:#818cf8,color:#fff,stroke-width:3px
    classDef event fill:#78350f,stroke:#fbbf24,color:#fff,stroke-width:2px
    classDef ops fill:#374151,stroke:#9ca3af,color:#fff,stroke-width:2px

    class client,ingress,gateway edge
    class user,restaurant,order,payment,delivery,statusListener,paymentListener,orderPaidListener,riderAssignedListener service
    class postgres,userdb,restaurantdb,orderdb,outbox,deliverydb database
    class kafka broker
    class placed,requested,completed,assigned,status,paid event
    class prometheus,grafana ops

```

Solid arrows are synchronous HTTP or persistence relationships. Dashed arrows are asynchronous Kafka publication, consumption, or metrics scraping.

## Important Flows

### Place and pay for an order

1. The client sends the request through the API gateway.
2. `order-service` persists the order and synchronously validates menu items with `restaurant-service`.
3. `order-service` synchronously calls `payment-service`. On success it sets order status to `PAID` and writes `order.placed.v1` and `order.paid.v1` to its `outbox_events` table in the same transaction — guaranteeing the DB update and the event intent are atomic.
4. `OutboxRelay` (a 1-second scheduler inside order-service) polls `outbox_events` for unsent rows, publishes each to Kafka via a `StringSerializer`-backed template, and marks the row sent on ack. `delivery-service` consumes `order.paid.v1` and creates the assignment; it then publishes `delivery.rider.assigned.v1`, which `order-service` consumes to set `deliveryStatus = ASSIGNED`. `payment-service` publishes `payment.requested.v1` and `payment.completed.v1` directly (stateless, no DB). `order-service` consumes `payment.completed.v1` as a reconciliation path.

### Delivery status updates

`delivery-service` persists each status transition and publishes `delivery.status.changed.v1`. `order-service` consumes that topic and updates its denormalized `deliveryStatus`; when the event is `DELIVERED`, the order status also becomes `DELIVERED`. Fetching an order also performs a synchronous delivery lookup, providing a reconciliation path in addition to the Kafka listener.

## Service and Data Store Map

| Service | HTTP port | Database | Kafka responsibility |
|---|---:|---|---|
| `api-gateway` | 8079 | — | — |
| `user-service` | 8081 | `userdb` | — |
| `restaurant-service` | 8082 | `restaurantdb` | — |
| `order-service` | 8083 | `orderdb` + `outbox_events` | Publishes `order.placed.v1` and `order.paid.v1` via outbox relay; consumes `payment.completed.v1`, `delivery.rider.assigned.v1`, and `delivery.status.changed.v1` |
| `payment-service` | 8084 | Stateless (no DB) | Publishes `payment.requested.v1` and `payment.completed.v1` |
| `delivery-service` | 8086 | `deliverydb` | Publishes `delivery.rider.assigned.v1` and `delivery.status.changed.v1`; consumes `order.paid.v1` |

The four service databases are separate PostgreSQL databases hosted by one PostgreSQL 15 instance. Services access one another by Compose/Kubernetes DNS names; clients use only the gateway.

## Kafka Topics

| Topic | Publisher | Consumer |
|---|---|---|
| `order.placed.v1` | `order-service` | No application consumer |
| `order.paid.v1` | `order-service` | `delivery-service` (trigger assignment) |
| `payment.requested.v1` | `payment-service` | No application consumer |
| `payment.completed.v1` | `payment-service` | `order-service` (reconciliation) |
| `delivery.rider.assigned.v1` | `delivery-service` | `order-service` (set deliveryStatus ASSIGNED) |
| `delivery.status.changed.v1` | `delivery-service` | `order-service` |

Kafka runs as a single-node KRaft broker without ZooKeeper. Containers and Kubernetes clients use `kafka:9092`; Docker Compose exposes `localhost:19092` for host access.

## Architecture Assessment

This diagram reflects the architecture that is implemented today. It is a sound MVP split: the gateway owns ingress and JWT enforcement, each stateful service owns a separate database, order orchestration uses explicit synchronous calls, and delivery status is propagated asynchronously.

For production scale, the main follow-up concerns are deliberate trade-offs rather than missing components:

- Kafka is single-node and PostgreSQL is a single instance, so both are availability bottlenecks.
- `order-service` uses a transactional outbox (`outbox_events` table + `OutboxRelay` scheduler) so its event publications are atomic with DB commits. `delivery-service` publishes events best-effort (no outbox yet), so a DB commit and event publication can still diverge there.
- Payment is synchronous; delivery assignment is event-driven (choreography saga). There is no compensation mechanism if delivery assignment fails after payment succeeds.
- Internal service ports are reachable inside the deployment network; network policies and service-level authorization would be needed for stronger isolation.

## Deployment Modes

**Docker Compose**

```bash
cp .env.example .env
mvn -DskipTests clean package
docker compose up --build
```

Gateway: `http://localhost:8079`. Prometheus and Grafana are available at `localhost:9090` and `localhost:3000`.

**Minikube**

```bash
eval $(minikube docker-env)
kubectl apply -f k8s/
kubectl port-forward -n food svc/api-gateway 8079:8079
```

**AWS EKS**

```bash
bash scripts/eks-up.sh
bash scripts/eks-down.sh
```

The `api-gateway` Service is patched to `type: LoadBalancer` by `eks-up.sh`, which provisions an AWS load balancer on port 80. The `30-ingress.yaml` Ingress manifest exists in the repo but is not applied by the EKS deploy script.

## Authentication and Health

The gateway validates HS256 JWTs for protected routes. `/`, `/health`, `/info`, `/actuator/**`, `POST /auth/register`, and `POST /auth/login` remain public; all other gateway routes require a token. `JWT_SECRET` must be shared by `api-gateway` and `user-service` and should be at least 256 bits.

Every service exposes health and Prometheus metrics through its actuator endpoints.
