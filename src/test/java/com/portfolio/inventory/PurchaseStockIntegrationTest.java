package com.portfolio.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.inventory.auth.JwtService;
import com.portfolio.inventory.catalog.*;
import com.portfolio.inventory.purchase.*;
import com.portfolio.inventory.stock.InventoryStockRepository;
import com.portfolio.inventory.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PurchaseStockIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwt;
    @Autowired ProductRepository products;
    @Autowired SupplierRepository suppliers;
    @Autowired WarehouseRepository warehouses;
    @Autowired PurchaseOrderRepository purchases;
    @Autowired InventoryStockRepository stocks;
    @Autowired StockReceiptRepository receipts;

    String buyerToken;
    String managerToken;
    String viewerToken;
    Long productId;
    Long supplierId;
    Long warehouseId;

    @BeforeEach
    void setUp() {
        receipts.deleteAll();
        purchases.deleteAll();
        stocks.deleteAll();
        users.deleteAll();
        buyerToken = token("buyer@example.com", Role.PURCHASING_AGENT);
        managerToken = token("manager@example.com", Role.INVENTORY_MANAGER);
        viewerToken = token("viewer@example.com", Role.VIEWER);
        productId = products.findAll().getFirst().getId();
        supplierId = suppliers.findAll().getFirst().getId();
        warehouseId = warehouses.findAll().getFirst().getId();
    }

    @Test
    void completePurchaseOrderWorkflowUpdatesStockOnlyOnReceipt() throws Exception {
        long id = create("PO-WORKFLOW");
        assertStatus(id, "DRAFT");
        assertStockCount(0);

        transition(id, "submit", buyerToken).andExpect(jsonPath("$.status").value("SUBMITTED"));
        transition(id, "approve", managerToken).andExpect(jsonPath("$.status").value("APPROVED"));
        assertStockCount(0);

        receive(id, 4).andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(4))
                .andExpect(jsonPath("$.items[0].outstandingQuantity").value(6));
        assertStockQuantity(4);

        receive(id, 6).andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].outstandingQuantity").value(0));
        assertStockQuantity(10);
        mvc.perform(get("/api/purchases/{id}/receipts", id)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quantity").value(4))
                .andExpect(jsonPath("$[1].quantity").value(6));
    }

    @Test
    void rejectsInvalidTransitionsAndOverReceipt() throws Exception {
        long id = create("PO-INVALID");
        transition(id, "approve", managerToken).andExpect(status().isConflict());
        transition(id, "submit", buyerToken).andExpect(status().isOk());
        transition(id, "approve", managerToken).andExpect(status().isOk());
        receive(id, 11).andExpect(status().isBadRequest());
        transition(id, "cancel", buyerToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        receive(id, 1).andExpect(status().isConflict());
    }

    @Test
    void enforcesWorkflowRolesAndListsByStatus() throws Exception {
        long id = create("PO-ROLES");
        transition(id, "submit", viewerToken).andExpect(status().isForbidden());
        transition(id, "submit", buyerToken).andExpect(status().isOk());
        transition(id, "approve", buyerToken).andExpect(status().isForbidden());

        mvc.perform(get("/api/purchases").param("status", "SUBMITTED")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    private long create(String reference) throws Exception {
        String response = mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"%s","supplierId":%d,"warehouseId":%d,
                                 "items":[{"productId":%d,"quantity":10,"unitCost":12.50}]}
                                """.formatted(reference, supplierId, warehouseId, productId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions transition(
            long id, String action, String token) throws Exception {
        return mvc.perform(post("/api/purchases/{id}/{action}", id, action)
                        .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions receive(long id, long quantity)
            throws Exception {
        return mvc.perform(post("/api/purchases/{id}/receive", id)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":" + productId
                        + ",\"quantity\":" + quantity + "}]}"));
    }

    private void assertStatus(long id, String expected) throws Exception {
        mvc.perform(get("/api/purchases/{id}", id)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expected));
    }

    private void assertStockCount(int count) throws Exception {
        mvc.perform(get("/api/stocks").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(count));
    }

    private void assertStockQuantity(long quantity) throws Exception {
        mvc.perform(get("/api/stocks").header("Authorization", "Bearer " + viewerToken)
                        .param("warehouseId", warehouseId.toString())
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantity").value(quantity));
    }

    private String token(String email, Role role) {
        UserAccount user = users.save(new UserAccount(email,
                passwordEncoder.encode("password123"), email, role));
        return jwt.create(user);
    }
}
