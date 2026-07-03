package com.tw.joi.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class E2EBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetState() {
        SeedData.orders.clear();
        SeedData.payments.clear();
        SeedData.notifications.clear();
        SeedData.trackingEvents.clear();
        SeedData.feedbacks.clear();
        SeedData.idempotencyStore.clear();

        // Reset cart to empty
        SeedData.cartForUsers.get("user101").setProducts(new ArrayList<>());

        // Reset product stock to 30
        SeedData.groceryProducts.forEach(p -> p.setAvailableStock(30));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Full happy path — browse → cart → order → pay → refund
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E: browse product, add to cart, place order, complete delivery, pay and refund")
    void fullHappyPath() throws Exception {

        // 1. List products for store101
        mockMvc.perform(MockMvcRequestBuilders.get("/products")
                            .param("storeId", "store101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].productId").exists());

        // 2. Search product by name
        mockMvc.perform(MockMvcRequestBuilders.get("/products/search")
                            .param("query", "wheat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productName").value("Wheat Bread"))
            .andExpect(jsonPath("$[0].storeId").value("store101"));

        // 3. Get product detail
        mockMvc.perform(MockMvcRequestBuilders.get("/products/{productId}", "product101")
                            .param("storeId", "store101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value("product101"))
            .andExpect(jsonPath("$.productName").value("Wheat Bread"))
            .andExpect(jsonPath("$.availableStock").value(30));

        // 4. Add product to cart
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"productId\":\"product101\",\"outletId\":\"store101\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cart.products[0].productId").value("product101"));

        // 5. View cart — product is there
        mockMvc.perform(MockMvcRequestBuilders.get("/cart/view")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products.length()").value(1))
            .andExpect(jsonPath("$.products[0].productId").value("product101"));

        // 6. Place order
        MvcResult placeResult = mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").exists())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalAmount").isNotEmpty())
            .andReturn();

        String orderId = objectMapper.readTree(placeResult.getResponse().getContentAsString())
            .get("orderId").asText();

        // 7. Verify "Order Confirmed" notification
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Order Confirmed"))
            .andExpect(jsonPath("$[0].read").value(false));

        // 8. Progress order: CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
        for (String nextStatus : new String[]{"PREPARING", "OUT_FOR_DELIVERY", "DELIVERED"}) {
            mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", orderId)
                                .param("userId", "user101")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"" + nextStatus + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(nextStatus));
        }

        // 9. Verify tracking history has 3 events
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        // 10. Latest tracking status is DELIVERED
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}/status", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DELIVERED"));

        // 11. Initiate payment (COD — always succeeds)
        MvcResult payResult = mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", "e2e-pay-" + orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + orderId + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andReturn();

        String paymentId = objectMapper.readTree(payResult.getResponse().getContentAsString())
            .get("paymentId").asText();

        // 12. Retry same payment with same idempotency key — must return same paymentId, no duplicate
        MvcResult retryResult = mockMvc.perform(MockMvcRequestBuilders.post("/payments/initiate")
                            .header("X-Idempotency-Key", "e2e-pay-" + orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"" + orderId + "\",\"userId\":\"user101\",\"method\":\"CASH_ON_DELIVERY\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String retryPaymentId = objectMapper.readTree(retryResult.getResponse().getContentAsString())
            .get("paymentId").asText();

        assertThat(retryPaymentId).isEqualTo(paymentId);
        assertThat(SeedData.payments).hasSize(1);

        // 13. "Payment Successful" notification present
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.title == 'Payment Successful')]").exists());

        // 14. Refund payment
        mockMvc.perform(MockMvcRequestBuilders.post("/payments/{paymentId}/refund", paymentId)
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REFUNDED"));

        // 15. "Payment Refunded" notification present
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.title == 'Payment Refunded')]").exists());

        // 16. Submit feedback on the delivered order
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"" + orderId + "\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Great service!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.type").value("ORDER"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Out-of-stock — inventory shows CRITICAL, order cancelled,
    //             cancellation notification validated
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E2E: product out of stock — inventory critical, order cancelled, notification received")
    void outOfStockCancellation() throws Exception {

        // 1. Set product101 stock to 0 — simulates inventory depletion
        SeedData.groceryProducts.stream()
            .filter(p -> "product101".equals(p.getProductId()))
            .findFirst()
            .orElseThrow()
            .setAvailableStock(0);

        // 2. Inventory health shows OUT_OF_STOCK for product101 and CRITICAL overall
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/health")
                            .param("storeId", "store101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallStatus").value("CRITICAL"))
            .andExpect(jsonPath("$.products[?(@.productId == 'product101')].stockStatus")
                           .value("OUT_OF_STOCK"));

        // 3. Customer adds the (out-of-stock) product to cart
        mockMvc.perform(MockMvcRequestBuilders.post("/cart/product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"productId\":\"product101\",\"outletId\":\"store101\"}"))
            .andExpect(status().isOk());

        // 4. Place order — order is accepted but will be cancelled due to stock issue
        MvcResult placeResult = mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andReturn();

        JsonNode orderNode = objectMapper.readTree(placeResult.getResponse().getContentAsString());
        String orderId = orderNode.get("orderId").asText();

        // 5. Verify "Order Confirmed" notification was received
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.title == 'Order Confirmed')]").exists());

        // 6. Operator cancels order due to stock unavailability
        mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{orderId}", orderId)
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 7. Verify "Order Cancelled" notification was received
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.title == 'Order Cancelled')]").exists());

        // 8. Confirm order is CANCELLED in the order list
        mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("CANCELLED"));

        // 9. Unread notifications count reflects both notifications (Order Confirmed + Cancelled)
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications/unread-count")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(2));
    }
}
