# Architecture Overview

```text
api-gateway  -->  user-service
              -->  restaurant-service
              -->  order-service

order-service  --(event: OrderCreated)-->  payment-service
payment-service --(event: PaymentCompleted)--> notification-service

All services register with service-discovery (Eureka).
```
