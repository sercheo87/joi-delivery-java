package com.tw.joi.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Payment;
import com.tw.joi.delivery.domain.PaymentMethod;
import com.tw.joi.delivery.domain.PaymentStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // "order-2".hashCode() % 10 != 0  →  card/UPI payment succeeds
    private static final String SUCCESS_ORDER_ID = "order-2";
    // "order-1".hashCode() % 10 == 0  →  card/UPI payment fails
    private static final String FAIL_ORDER_ID = "order-1";

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.payments.clear();
        SeedData.notifications.clear();
        SeedData.idempotencyStore.clear();

        Cart cart = SeedData.cartForUsers.get("user101");
        cart.setProducts(new ArrayList<>(SeedData.groceryProducts.subList(0, 1)));
    }

    private Order createConfirmedOrder(String orderId) {
        Order order = Order.builder()
            .orderId(orderId)
            .userId("user101")
            .outletId("store101")
            .products(new ArrayList<>())
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(10.5))
            .placedAt(LocalDateTime.now())
            .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(45))
            .build();
        SeedData.orders.add(order);
        return order;
    }

    // ── POST /payments/initiate ──────────────────────────────────────────────

    @Test
    void shouldInitiatePaymentSuccessfullyWithCod() throws Exception {
        createConfirmedOrder("order-cod");

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"order-cod\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").isNotEmpty())
            .andExpect(jsonPath("$.orderId").value("order-cod"))
            .andExpect(jsonPath("$.userId").value("user101"))
            .andExpect(jsonPath("$.method").value("CASH_ON_DELIVERY"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.amount").isNotEmpty())
            .andExpect(jsonPath("$.initiatedAt").isNotEmpty())
            .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void shouldInitiateCardPaymentSuccessfully() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CREDIT_CARD\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldFailCardPaymentWhenHashConditionMet() throws Exception {
        createConfirmedOrder(FAIL_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + FAIL_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CREDIT_CARD\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").isNotEmpty());
    }

    @Test
    void shouldReturn404WhenOrderNotFoundForPayment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"order-nonexistent\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenOrderBelongsToAnotherUserOnInitiate() throws Exception {
        createConfirmedOrder("order-cod");

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"order-cod\",\"userId\":\"user999\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenOrderIsCancelled() throws Exception {
        Order order = createConfirmedOrder("order-cod");
        order.setStatus(OrderStatus.CANCELLED);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"order-cod\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenOrderAlreadyPaid() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"DEBIT_CARD\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── GET /payments/order/{orderId} ────────────────────────────────────────

    @Test
    void shouldGetPaymentByOrder() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/payments/order/{orderId}", SUCCESS_ORDER_ID)
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(SUCCESS_ORDER_ID))
            .andExpect(jsonPath("$.userId").value("user101"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldReturn404WhenNoPaymentForOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/payments/order/{orderId}", "order-nonexistent")
                            .param("userId", "user101"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenGettingPaymentOfAnotherUser() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/payments/order/{orderId}", SUCCESS_ORDER_ID)
                            .param("userId", "user999"))
            .andExpect(status().isForbidden());
    }

    // ── POST /payments/{paymentId}/refund ────────────────────────────────────

    @Test
    void shouldRefundPaymentSuccessfully() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated());

        Payment payment = SeedData.payments.get(0);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/{paymentId}/refund", payment.getPaymentId())
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REFUNDED"))
            .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void shouldReturn404WhenRefundingNonExistentPayment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/payments/{paymentId}/refund", "payment-nonexistent")
                            .param("userId", "user101"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenRefundingPaymentOfAnotherUser() throws Exception {
        createConfirmedOrder(SUCCESS_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + SUCCESS_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated());

        Payment payment = SeedData.payments.get(0);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/{paymentId}/refund", payment.getPaymentId())
                            .param("userId", "user999"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenRefundingFailedPayment() throws Exception {
        createConfirmedOrder(FAIL_ORDER_ID);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + FAIL_ORDER_ID + "\",\"userId\":\"user101\",\"method\":\"CREDIT_CARD\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FAILED"));

        Payment payment = SeedData.payments.get(0);

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/{paymentId}/refund", payment.getPaymentId())
                            .param("userId", "user101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnSamePaymentIdWhenIdempotencyKeyReused() throws Exception {
        createConfirmedOrder("order-idem");
        String idempotencyKey = "fixed-key-123";
        String body = "{\"orderId\":\"order-idem\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isCreated());

        String firstPaymentId = SeedData.payments.get(0).getPaymentId();

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(firstPaymentId));

        assertThat(SeedData.payments).hasSize(1);
    }

    @Test
    void shouldInitiatePaymentViaPlaceOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        String orderId = SeedData.orders.get(0).getOrderId();

        mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", java.util.UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + orderId + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
