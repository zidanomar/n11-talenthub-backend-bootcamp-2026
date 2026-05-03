# Saga Documentation

The order lifecycle is implemented as a **choreography-based saga** — no central orchestrator. Services emit and consume events on a single RabbitMQ topic exchange (`events`); each service owns one slice of the flow.

---

## Table of Contents

- [Saga Documentation](#saga-documentation)
  - [Table of Contents](#table-of-contents)
  - [Exchange \& Conventions](#exchange--conventions)
  - [Routing Keys](#routing-keys)
    - [Business Events](#business-events)
    - [Notification Events (order → notification)](#notification-events-order--notification)
  - [Queues \& Bindings](#queues--bindings)
  - [Saga 1 — Order Payment](#saga-1--order-payment)
    - [Pre-Saga: Order Placement](#pre-saga-order-placement)
    - [Happy Path](#happy-path)
    - [Compensation (Payment Rejected)](#compensation-payment-rejected)
  - [Saga 2 — Order Return](#saga-2--order-return)
    - [Happy Path](#happy-path-1)
    - [Compensation (Refund Failed)](#compensation-refund-failed)
    - [Alternative Terminal: Keep the Item](#alternative-terminal-keep-the-item)
  - [Saga 3 — Order Cancel](#saga-3--order-cancel)
  - [Notifications](#notifications)
  - [Order Status Lifecycle](#order-status-lifecycle)
  - [Known Gaps](#known-gaps)

---

## Exchange & Conventions

| Property   | Value                                       |
| ---------- | ------------------------------------------- |
| Exchange   | `events`                                    |
| Type       | Topic                                       |
| Durability | durable                                     |
| Payload    | JSON, serialized via Jackson `ObjectMapper` |

Conventions:

- Event payloads always include `orderId` (Long).
- User-scoped notifications also include `userId` (String, Keycloak sub).
- Failure events include a `reason` field (String, may be null).
- Routing keys are dot-separated: `<domain>.<event>` for business events, `notification.<event>` for UI notifications.

---

## Routing Keys

### Business Events

| Routing Key           | Publisher | Consumers       | Purpose                          |
| --------------------- | --------- | --------------- | -------------------------------- |
| `order.created`       | order     | —               | Reserved (no current consumer)   |
| `payment.accepted`    | payment   | order, shipping | Payment verified, order paid     |
| `payment.rejected`    | payment   | order           | Payment failed                   |
| `shipping.processing` | shipping  | order           | Carrier picked up package        |
| `shipping.delivering` | shipping  | —               | Out for delivery (informational) |
| `shipping.delivered`  | shipping  | order           | Customer received package        |
| `return.requested`    | order     | shipping        | Customer initiated return        |
| `return.shipped`      | shipping  | order           | Return picked up                 |
| `return.delivered`    | shipping  | order           | Return arrived at warehouse      |
| `refund.initiate`     | order     | payment         | Trigger refund processing        |
| `refund.completed`    | payment   | order           | Refund successful                |
| `refund.failed`       | payment   | order           | Refund could not be processed    |

### Notification Events (order → notification)

| Routing Key                     | Trigger                             |
| ------------------------------- | ----------------------------------- |
| `notification.order.created`    | Order placed (`PENDING`)            |
| `notification.payment.accepted` | Payment accepted                    |
| `notification.payment.rejected` | Payment rejected                    |
| `notification.order.shipped`    | Order → SHIPPED                     |
| `notification.order.delivered`  | Order → DELIVERED                   |
| `notification.return.initiated` | Return initiated                    |
| `notification.return.shipped`   | Return picked up                    |
| `notification.refunding`        | Return at warehouse, refund started |
| `notification.returned`         | Refund completed                    |
| `notification.return.failed`    | Refund failed                       |

---

## Queues & Bindings

| Queue                                 | Bound Routing Key(s)                        | Consumer     |
| ------------------------------------- | ------------------------------------------- | ------------ |
| `shipping.queue`                      | `payment.accepted`                          | shipping     |
| `shipping.return.queue`               | `return.requested`                          | shipping     |
| `order.payment-accepted.queue`        | `payment.accepted`                          | order        |
| `order.payment-rejected.queue`        | `payment.rejected`                          | order        |
| `order.shipping-update.queue`         | `shipping.processing`, `shipping.delivered` | order        |
| `order.return-shipped.queue`          | `return.shipped`                            | order        |
| `order.return-delivered.queue`        | `return.delivered`                          | order        |
| `order.refund-completed.queue`        | `refund.completed`                          | order        |
| `order.refund-failed.queue`           | `refund.failed`                             | order        |
| `payment.refund-initiate.queue`       | `refund.initiate`                           | payment      |
| `notification.order-created.queue`    | `notification.order.created`                | notification |
| `notification.payment-accepted.queue` | `notification.payment.accepted`             | notification |
| `notification.payment-rejected.queue` | `notification.payment.rejected`             | notification |
| `notification.order-shipped.queue`    | `notification.order.shipped`                | notification |
| `notification.order-delivered.queue`  | `notification.order.delivered`              | notification |
| `notification.return-initiated.queue` | `notification.return.initiated`             | notification |
| `notification.return-shipped.queue`   | `notification.return.shipped`               | notification |
| `notification.refunding.queue`        | `notification.refunding`                    | notification |
| `notification.returned.queue`         | `notification.returned`                     | notification |
| `notification.return-failed.queue`    | `notification.return.failed`                | notification |

All queues are durable. Routing-key strings are centralized in `apps/config/src/main/resources/config/application.yml` and loaded by each service via Spring Cloud Config.

---

## Saga 1 — Order Payment

**Trigger:** `POST /api/orders` (placement) followed by `POST /api/orders/{id}/pay`.

### Pre-Saga: Order Placement

Synchronous, no async events except the customer notification.

```
Customer ──► order.placeOrder
              │
              ├─ cartClient.getCart()                  [Feign → cart]
              ├─ save Order (PENDING)                  [DB]
              ├─ cartClient.clearCart()                [Feign → cart]
              └─ publish notification.order.created    [AMQP]

notification ──► onOrderCreated → persist + push SSE
```

### Happy Path

```
Customer ──► POST /api/orders/{id}/pay
              │
              ├─ productClient.deductStock(...)        [Feign → product, per item]
              ├─ paymentClient.initiate(...)           [Feign → payment]
              └─ return { paymentPageUrl, token }

Customer redirected to iyzipay hosted page → enters card.

iyzipay ──► POST /api/payments/callback/{method}
              │
              ├─ provider.verify(token)                [iyzipay API]
              ├─ save Payment (iyzipayPaymentId)       [DB]
              └─ publish payment.accepted              [AMQP, after commit]

shipping ──► onPaymentAccepted (shipping.queue)
              ├─ sleep 10–30s; publish shipping.processing
              ├─ sleep 10–30s; publish shipping.delivering
              └─ sleep 10–30s; publish shipping.delivered

order ──► onPaymentAccepted (order.payment-accepted.queue)
              └─ status PENDING → PAID
                 publish notification.payment.accepted

order ──► onShippingUpdate (order.shipping-update.queue)
              ├─ shipping.processing → status PAID → SHIPPED
              │                        publish notification.order.shipped
              └─ shipping.delivered  → status SHIPPED → DELIVERED
                                       publish notification.order.delivered

notification ──► consumes notification.* → persist + push SSE
```

### Compensation (Payment Rejected)

```
iyzipay ──► callback → provider.verify(token) → rejected
payment ──► publish payment.rejected { orderId, reason }

order ──► onPaymentRejected
              ├─ productClient.restoreStock(...)       [Feign, per item, best-effort]
              ├─ status PENDING → PAYMENT_FAILED
              └─ publish notification.payment.rejected

Customer can retry via POST /api/orders/{id}/pay.
```

---

## Saga 2 — Order Return

**Trigger:** `POST /api/orders/{id}/return` (allowed only when order is `DELIVERED`).

### Happy Path

```
Customer ──► POST /api/orders/{id}/return
              ├─ status DELIVERED → RETURNING
              ├─ publish return.requested
              └─ publish notification.return.initiated

shipping ──► onReturnRequested (shipping.return.queue)
              ├─ sleep 10–30s; publish return.shipped
              └─ sleep 10–30s; publish return.delivered

order ──► onReturnShipped → status RETURNING → RETURN_SHIPPED
                            publish notification.return.shipped

order ──► onReturnDelivered → status RETURN_SHIPPED → REFUNDING
                              publish refund.initiate
                              publish notification.refunding

payment ──► onRefundInitiate (payment.refund-initiate.queue)
              ├─ load Payment.iyzipayPaymentId by orderId
              ├─ provider.refund(...)                  [iyzipay API]
              └─ publish refund.completed

order ──► onRefundCompleted → restore stock (Feign, best-effort)
                              status REFUNDING → RETURNED
                              publish notification.returned
```

### Compensation (Refund Failed)

```
payment ──► refund call fails
              └─ publish refund.failed { orderId, reason }

order ──► onRefundFailed
              ├─ status REFUNDING → RETURN_FAILED
              ├─ persist `reason` in OrderStatusHistory
              └─ publish notification.return.failed

Customer ──► POST /api/orders/{id}/retry-refund (RETURN_FAILED only)
              ├─ status RETURN_FAILED → REFUNDING
              └─ publish refund.initiate (same path resumes)
```

### Alternative Terminal: Keep the Item

```
Customer ──► POST /api/orders/{id}/finish (DELIVERED only, no events)
              └─ status DELIVERED → COMPLETED
```

---

## Saga 3 — Order Cancel

**Trigger:** `POST /api/orders/{id}/cancel` (allowed only when order is `PENDING` or `PAYMENT_FAILED`).

Single synchronous step. No events, no compensation.

```
Customer ──► order.cancel
              └─ status → CANCELLED
```

Why no compensation:

- `PENDING` — stock was never deducted (deduction happens in `payOrder`, not `placeOrder`).
- `PAYMENT_FAILED` — stock was already restored by the payment-rejected compensation handler.

---

## Notifications

The notification service is a pure consumer. It never publishes events. For every `notification.*` routing key it:

1. Persists a `Notification` row (type, title, message, orderId, userId).
2. Pushes the new notification to the connected client via SSE (`SseEmitterRegistry`).

| Event                           | `NotificationType` | Title             | Message                                       |
| ------------------------------- | ------------------ | ----------------- | --------------------------------------------- |
| `notification.order.created`    | `ORDER_CREATED`    | Order Placed      | Order #N was placed and is awaiting payment.  |
| `notification.payment.accepted` | `PAYMENT_ACCEPTED` | Payment Confirmed | Your payment for order #N was accepted.       |
| `notification.payment.rejected` | `PAYMENT_REJECTED` | Payment Failed    | Payment for order #N failed: `<reason>`.      |
| `notification.order.shipped`    | `ORDER_SHIPPED`    | Order Shipped     | Order #N is on its way!                       |
| `notification.order.delivered`  | `ORDER_DELIVERED`  | Order Delivered   | Order #N has been delivered.                  |
| `notification.return.initiated` | `RETURN_INITIATED` | Return Initiated  | Return for order #N has been initiated.       |
| `notification.return.shipped`   | `RETURN_SHIPPED`   | Return Picked Up  | Your return for order #N is in transit.       |
| `notification.refunding`        | `REFUNDING`        | Refund Processing | Returned package received. Refund processing. |
| `notification.returned`         | `RETURNED`         | Refund Completed  | Your refund for order #N is complete.         |
| `notification.return.failed`    | `RETURN_FAILED`    | Refund Failed     | Refund for order #N failed: `<reason>`.       |

---

## Order Status Lifecycle

```
PENDING ──► PAID ──► SHIPPED ──► DELIVERED ──► COMPLETED
   │                                │
   │                                └──► RETURNING ──► RETURN_SHIPPED ──► REFUNDING ──► RETURNED
   │                                                                          │
   │                                                                          └──► RETURN_FAILED
   │                                                                                  │ (retry)
   │                                                                                  ▼
   │                                                                              REFUNDING ...
   │
   ├──► PAYMENT_FAILED   (stock restored, retryable)
   └──► CANCELLED        (terminal, only from PENDING / PAYMENT_FAILED)
```

---

## Known Gaps

| Gap                                             | Saga   | Impact                                                                                                                                 |
| ----------------------------------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------------------- |
| Stock leak if `paymentClient.initiate()` throws | Saga 1 | `productClient.deductStock` already ran; no compensation restores it. Manual reconciliation required.                                  |
| Cannot cancel `PAID` / `SHIPPED` orders         | —      | Would need a refund + stock-restore choreography (not implemented).                                                                    |
| Publish-before-commit in `OrderServiceImpl`     | All    | Most `publish*` calls happen inside `@Transactional` methods. Only `payment` uses `afterCommit`. Failure window is small but non-zero. |
