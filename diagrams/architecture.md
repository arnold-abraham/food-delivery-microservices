# Architecture Overview

```text

+-------------------+   routes   +-------------------+
|    api-gateway    +----------->|   user-service    |
| (Spring Gateway)  |            |  (Postgres:userdb)|
+-------------------+            +-------------------+
        | routes
        +-----------------------> +-------------------+
        |                         | restaurant-service |
        |                         | (Postgres:restdb) |
        |                         +-------------------+
        | routes
        +-----------------------> +-------------------+
        |                         |   order-service   |
        |                         | (Postgres:orderdb)|
        |                         +---------+---------+
        |                                   |
        |                                   | HTTP POST /payments
        |                                   v
        |                         +-------------------+
        |                         |  payment-service  |
        |                         +-------------------+
        |
        | routes
        +-----------------------> +-------------------+
                                  | delivery-service  |
                                  | (Postgres:delivdb)|
                                  +-------------------+

Notes:
- Services discover each other via DNS (Docker Compose service names / Kubernetes Service DNS).
- Only the API Gateway is exposed to callers; internal services stay private.
```
