# JOI Delivery — Development Guide

## Business Flow

End-to-end journey of a customer from discovering products to receiving their delivery.

```mermaid
flowchart TD
    A([🛒 Customer opens app]) --> B[Browse stores & products\nGET /products?storeId=]
    B --> C{Found what\nthey need?}
    C -- No --> B
    C -- Yes --> D[Search by name\nGET /products/search?query=]
    D --> E[Add product to cart\nPOST /cart/product]
    E --> F{Add more\nproducts?}
    F -- Yes --> B
    F -- No --> G[Review cart\nGET /cart/view]

    G --> H[Place order\nPOST /orders/place]
    H --> I[🔔 Notification: Order Confirmed]
    I --> J[Choose payment method\nCredit Card / UPI / Cash on Delivery]
    J --> K[Initiate payment\nPOST /payments/initiate\nheader: X-Idempotency-Key]

    K --> L{Payment\nsuccessful?}
    L -- No --> M[🔔 Notification: Payment Failed]
    M --> N[Retry with same\nIdempotency Key]
    N --> K

    L -- Yes --> O[🔔 Notification: Payment Successful]
    O --> P[Order being prepared\nStatus: PREPARING]
    P --> Q[🔔 Notification: Preparing your order]
    Q --> R[Rider picks up order\nStatus: OUT_FOR_DELIVERY]
    R --> S[🔔 Notification: Order on its way]
    S --> T[Track in real time\nGET /tracking/orderId/status]
    T --> U[Order delivered\nStatus: DELIVERED]
    U --> V[🔔 Notification: Order Delivered]

    V --> W{Need a\nrefund?}
    W -- Yes --> X[Request refund\nPOST /payments/paymentId/refund]
    X --> Y[🔔 Notification: Payment Refunded]
    W -- No --> FB[Leave feedback & rating\nPOST /feedback]
    Y --> FB
    FB --> Z([✅ Done])

    style A fill:#4CAF50,color:#fff
    style Z fill:#4CAF50,color:#fff
    style I fill:#2196F3,color:#fff
    style O fill:#2196F3,color:#fff
    style Q fill:#2196F3,color:#fff
    style S fill:#2196F3,color:#fff
    style V fill:#2196F3,color:#fff
    style Y fill:#2196F3,color:#fff
    style M fill:#f44336,color:#fff
```

> **🔔 Notifications** are generated automatically at every key transition — the customer is always informed without having to poll for status.
>
> **🔑 Idempotency Key** — `POST /payments/initiate` requires a `X-Idempotency-Key` header. Retrying with the same key returns the cached result without creating a duplicate payment.

---

## Architecture Overview

JOI Delivery is a Spring Boot 3.5.3 / Java 25 REST API. All data lives in-memory via `SeedData` — there is no database. The application is structured in three layers: Controllers → Services → Domain/SeedData.

---

## C4 — Level 1: System Context

```mermaid
C4Context
    title JOI Delivery — System Context

    Person(customer, "Customer", "Places orders, tracks deliveries, manages payments")
    Person(vendor, "Store / Vendor", "Manages inventory and order status")

    System(joi, "JOI Delivery API", "Spring Boot REST API. Handles products, cart, orders, tracking, payments and notifications.")

    Rel(customer, joi, "Uses", "HTTP/REST")
    Rel(vendor, joi, "Updates order status", "HTTP/REST")
```

---

## C4 — Level 2: Container

```mermaid
C4Container
    title JOI Delivery — Container Diagram

    Person(customer, "Customer")
    Person(vendor, "Vendor")

    Container_Boundary(api, "JOI Delivery API") {
        Container(app, "Spring Boot App", "Java 25 / Spring Boot 3.5.3", "Exposes REST endpoints on port 8080")
        ContainerDb(mem, "In-Memory Store", "SeedData.java", "Static lists for users, stores, products, carts, orders, payments, notifications, tracking events")
    }

    Rel(customer, app, "HTTP REST", "JSON")
    Rel(vendor, app, "HTTP REST", "JSON")
    Rel(app, mem, "Reads / writes")
```

---

## C4 — Level 3: Component

```mermaid
C4Component
    title JOI Delivery — Component Diagram

    Container_Boundary(api, "Spring Boot Application") {

        Component(pc, "ProductController", "/products", "Browse and search grocery products")
        Component(cc, "CartController", "/cart", "Add products to cart, view cart")
        Component(ic, "InventoryController", "/inventory", "Check store inventory health")
        Component(oc, "OrderController", "/orders", "Place, list, cancel and update order status")
        Component(tc, "TrackingController", "/tracking", "View order tracking history and latest status")
        Component(paymC, "PaymentController", "/payments", "Initiate, query and refund payments")
        Component(nc, "NotificationController", "/notifications", "View, mark-read and count unread notifications")
        Component(fc, "FeedbackController", "/feedback", "Submit ratings and comments, view feedback by user or order")

        Component(ps, "ProductService", "", "getProductsByStore, searchProducts, getProductDetail")
        Component(cs, "CartService", "", "addProductToCartForUser, getCartForUser")
        Component(is, "InventoryService", "", "getInventoryHealth")
        Component(os, "OrderService", "", "placeOrder, cancelOrder, updateOrderStatus")
        Component(ts, "TrackingService", "", "getTrackingHistory, getLatestStatus")
        Component(pays, "PaymentService", "", "initiatePayment, getPaymentByOrder, refundPayment")
        Component(ns, "NotificationService", "", "getNotificationsForUser, markAsRead, getUnreadCount")
        Component(fbs, "FeedbackService", "", "submitFeedback, getFeedbackByUser, getFeedbackByOrder, getAverageRating")
        Component(ss, "StoreService", "", "findById")
        Component(us, "UserService", "", "fetchUserById")

        ComponentDb(seed, "SeedData", "", "Static in-memory collections: users, stores, products, carts, orders, payments, notifications, trackingEvents, feedbacks")

        Rel(pc, ps, "uses")
        Rel(cc, cs, "uses")
        Rel(ic, is, "uses")
        Rel(oc, os, "uses")
        Rel(tc, ts, "uses")
        Rel(paymC, pays, "uses")
        Rel(nc, ns, "uses")
        Rel(fc, fbs, "uses")

        Rel(ps, ss, "uses")
        Rel(is, ss, "uses")
        Rel(is, ps, "uses")
        Rel(cs, us, "uses")
        Rel(os, cs, "uses")
        Rel(os, ns, "creates notifications")
        Rel(os, seed, "reads/writes orders, trackingEvents")
        Rel(pays, os, "reads orders")
        Rel(pays, ns, "creates notifications")
        Rel(pays, seed, "reads/writes payments")
        Rel(ts, seed, "reads orders, trackingEvents")
        Rel(ns, seed, "reads/writes notifications")
        Rel(fbs, seed, "reads/writes feedbacks")
        Rel(fbs, us, "validates user")
        Rel(fbs, os, "validates order")
        Rel(ss, seed, "reads stores")
        Rel(us, seed, "reads users")
        Rel(ps, seed, "reads products")
        Rel(cs, seed, "reads/writes carts")
    }
```

---

## Architectural Patterns

### Idempotency Key — Payment fault tolerance

`POST /payments/initiate` implements the **Idempotency Key** pattern to prevent duplicate charges on client retries (network timeouts, app crashes mid-request).

**How it works:**

```
Client                          API                        idempotencyStore (Map)
  │                              │                                │
  │── POST /payments/initiate ──►│                                │
  │   X-Idempotency-Key: abc123  │── get("abc123") ─────────────►│ (miss)
  │                              │                                │
  │                              │  [process payment]             │
  │                              │── put("abc123", payment) ─────►│
  │◄─ 201 { paymentId: "p-1" } ──│                                │
  │                              │                                │
  │  [timeout / retry]           │                                │
  │── POST /payments/initiate ──►│                                │
  │   X-Idempotency-Key: abc123  │── get("abc123") ─────────────►│ (hit)
  │◄─ 201 { paymentId: "p-1" } ──│◄── return cached payment ──────│
  │   (same result, no charge)   │                                │
```

**Rules:**
- Same key → same response, no second charge, `SeedData.payments` stays at size 1
- Different key → new payment processed independently
- The store is a `ConcurrentHashMap<String, Payment>` in `SeedData.idempotencyStore`

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/products?storeId=` | List products for a store |
| `GET` | `/products/search?query=` | Search products by name (all stores) |
| `GET` | `/products/{productId}?storeId=` | Get product detail |
| `POST` | `/cart/product` | Add product to cart |
| `GET` | `/cart/view?userId=` | View cart |
| `GET` | `/inventory/health?storeId=` | Inventory health for a store |
| `POST` | `/orders/place?userId=` | Place order from cart |
| `GET` | `/orders?userId=` | List orders for a user |
| `DELETE` | `/orders/{orderId}?userId=` | Cancel an order |
| `PATCH` | `/orders/{orderId}/status?userId=` | Update order status |
| `GET` | `/tracking/{orderId}` | Full tracking history for an order |
| `GET` | `/tracking/{orderId}/status` | Latest tracking status |
| `POST` | `/payments/initiate` | Initiate a payment _(requires `X-Idempotency-Key` header)_ |
| `GET` | `/payments/order/{orderId}?userId=` | Get payment for an order |
| `POST` | `/payments/{paymentId}/refund?userId=` | Refund a payment |
| `GET` | `/notifications?userId=` | List notifications |
| `GET` | `/notifications/unread-count?userId=` | Count unread notifications |
| `PATCH` | `/notifications/{id}/read?userId=` | Mark notification as read |
| `PATCH` | `/notifications/read-all?userId=` | Mark all notifications as read |
| `POST` | `/feedback` | Submit a rating and comment |
| `GET` | `/feedback/user?userId=` | Get all feedback submitted by a user |
| `GET` | `/feedback/order/{orderId}` | Get all feedback for a specific order |
| `GET` | `/feedback/store/{storeId}/rating` | Get average rating for a store |

---

## Domain Model

```mermaid
classDiagram
    class User {
        String userId
        String firstName
        String lastName
        String email
        String phoneNumber
        Cart cart
    }

    class Cart {
        String cartId
        Outlet outlet
        List~Product~ products
        User user
    }

    class Product {
        <<abstract>>
        String productId
        String productName
        BigDecimal mrp
    }

    class GroceryProduct {
        BigDecimal sellingPrice
        Double weight
        Integer availableStock
        Integer threshold
        GroceryStore store
    }

    class Order {
        String orderId
        String userId
        String outletId
        List~Product~ products
        OrderStatus status
        BigDecimal totalAmount
        LocalDateTime placedAt
        LocalDateTime estimatedDeliveryTime
    }

    class OrderStatus {
        <<enumeration>>
        CONFIRMED
        PREPARING
        OUT_FOR_DELIVERY
        DELIVERED
        CANCELLED
    }

    class TrackingEvent {
        String eventId
        String orderId
        OrderStatus status
        String message
        LocalDateTime timestamp
    }

    class Payment {
        String paymentId
        String orderId
        String userId
        BigDecimal amount
        PaymentMethod method
        PaymentStatus status
        LocalDateTime initiatedAt
        LocalDateTime completedAt
    }

    class PaymentStatus {
        <<enumeration>>
        PENDING
        SUCCESS
        FAILED
        REFUNDED
    }

    class PaymentMethod {
        <<enumeration>>
        CREDIT_CARD
        DEBIT_CARD
        UPI
        CASH_ON_DELIVERY
    }

    class Notification {
        String notificationId
        String userId
        String orderId
        String title
        String message
        boolean read
        LocalDateTime createdAt
    }

    class GroceryStore {
        String outletId
        String name
        Set~GroceryProduct~ inventory
    }

    class Feedback {
        String feedbackId
        String userId
        String orderId
        FeedbackType type
        int rating
        String comment
        LocalDateTime submittedAt
    }

    class FeedbackType {
        <<enumeration>>
        ORDER
        DELIVERY
        PRODUCT
        APP
    }

    User "1" --> "1" Cart
    Cart "1" --> "*" Product
    Product <|-- GroceryProduct
    GroceryProduct --> GroceryStore
    Order --> OrderStatus
    Order "1" --> "*" TrackingEvent : generates
    Order "1" --> "0..1" Payment
    Payment --> PaymentStatus
    Payment --> PaymentMethod
    User "1" --> "*" Notification
    User "1" --> "*" Order
    User "1" --> "*" Feedback
    Order "1" --> "*" Feedback
    Feedback --> FeedbackType
```

---

## Order Lifecycle

```mermaid
stateDiagram-v2
    [*] --> CONFIRMED : POST /orders/place
    CONFIRMED --> PREPARING : PATCH /orders/{id}/status
    PREPARING --> OUT_FOR_DELIVERY : PATCH /orders/{id}/status
    OUT_FOR_DELIVERY --> DELIVERED : PATCH /orders/{id}/status
    CONFIRMED --> CANCELLED : DELETE /orders/{id}
    PREPARING --> CANCELLED : DELETE /orders/{id}
```

---

## Payment Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING : POST /payments/initiate
    PENDING --> SUCCESS : Payment processed OK
    PENDING --> FAILED : Payment declined
    SUCCESS --> REFUNDED : POST /payments/{id}/refund
```

---

## Seed Data (Development)

| Entity | Id | Detail |
|--------|----|--------|
| User | `user101` | John Doe — john.doe@gmail.com |
| Store | `store101` | Fresh Picks |
| Store | `store102` | Natural Choice |
| Product | `product101` | Wheat Bread — store101 |
| Product | `product102` | Spinach — store101 |
| Product | `product103` | Crackers — store101 |
| Product | `product104` | Organic Milk — store102 |
| Product | `product105` | Brown Rice — store102 |

---

## Running the project

```bash
# Build and test
./gradlew build

# Run (port 8080)
./gradlew bootRun

# Unit tests only
./gradlew test

# Unit + functional tests
./gradlew check
```
