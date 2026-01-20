# Architecture Overview

```text
                +-------------------+
                | service-discovery |
                |    (Eureka)       |
                +---------+---------+
                          ^
                 registers|discovers
                          v
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
                                  |(Postgres:deliverydb)|
                                  +-------------------+

Notes:
- Postgres is a single container, but we use separate databases per service.
- Order -> Payment is synchronous HTTP for the base version.
```
