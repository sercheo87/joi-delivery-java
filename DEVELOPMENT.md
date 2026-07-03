# JOI Delivery — Development Guide

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

        Component(ps, "ProductService", "", "getProductsByStore, searchProducts, getProductDetail")
        Component(cs, "CartService", "", "addProductToCartForUser, getCartForUser")
        Component(is, "InventoryService", "", "getInventoryHealth")
        Component(os, "OrderService", "", "placeOrder, cancelOrder, updateOrderStatus")
        Component(ts, "TrackingService", "", "getTrackingHistory, getLatestStatus")
        Component(pays, "PaymentService", "", "initiatePayment, getPaymentByOrder, refundPayment")
        Component(ns, "NotificationService", "", "getNotificationsForUser, markAsRead, getUnreadCount")
        Component(ss, "StoreService", "", "findById")
        Component(us, "UserService", "", "fetchUserById")

        ComponentDb(seed, "SeedData", "", "Static in-memory collections: users, stores, products, carts, orders, payments, notifications, trackingEvents")

        Rel(pc, ps, "uses")
        Rel(cc, cs, "uses")
        Rel(ic, is, "uses")
        Rel(oc, os, "uses")
        Rel(tc, ts, "uses")
        Rel(paymC, pays, "uses")
        Rel(nc, ns, "uses")

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
        Rel(ss, seed, "reads stores")
        Rel(us, seed, "reads users")
        Rel(ps, seed, "reads products")
        Rel(cs, seed, "reads/writes carts")
    }
```

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
| `POST` | `/payments/initiate` | Initiate a payment |
| `GET` | `/payments/order/{orderId}?userId=` | Get payment for an order |
| `POST` | `/payments/{paymentId}/refund?userId=` | Refund a payment |
| `GET` | `/notifications?userId=` | List notifications |
| `GET` | `/notifications/unread-count?userId=` | Count unread notifications |
| `PATCH` | `/notifications/{id}/read?userId=` | Mark notification as read |
| `PATCH` | `/notifications/read-all?userId=` | Mark all notifications as read |

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
