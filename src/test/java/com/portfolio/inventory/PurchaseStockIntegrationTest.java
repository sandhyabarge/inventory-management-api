package com.portfolio.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.portfolio.inventory.auth.JwtService;
import com.portfolio.inventory.catalog.*;
import com.portfolio.inventory.purchase.StockPurchaseRepository;
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
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwt;
    @Autowired ProductRepository products;
    @Autowired SupplierRepository suppliers;
    @Autowired WarehouseRepository warehouses;
    @Autowired StockPurchaseRepository purchases;
    @Autowired InventoryStockRepository stocks;

    private String purchasingToken;
    private String viewerToken;
    private Long productId;
    private Long supplierId;
    private Long warehouseId;

    @BeforeEach
    void setUp() {
        purchases.deleteAll();
        stocks.deleteAll();
        users.deleteAll();
        UserAccount agent = users.save(new UserAccount(
                "buyer@example.com", passwordEncoder.encode("password123"),
                "Buyer", Role.PURCHASING_AGENT));
        UserAccount viewer = users.save(new UserAccount(
                "viewer@example.com", passwordEncoder.encode("password123"),
                "Viewer", Role.VIEWER));
        purchasingToken = jwt.create(agent);
        viewerToken = jwt.create(viewer);
        productId = products.findAll().getFirst().getId();
        supplierId = suppliers.findAll().getFirst().getId();
        warehouseId = warehouses.findAll().getFirst().getId();
    }

    @Test
    void purchaseImmediatelyAddsAvailableStock() throws Exception {
        mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + purchasingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("PO-1001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("PO-1001"))
                .andExpect(jsonPath("$.totalCost").value(125.00))
                .andExpect(jsonPath("$.items[0].quantity").value(10));

        mvc.perform(get("/api/stocks")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("warehouseId", warehouseId.toString())
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(10));

        mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + purchasingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("PO-1002")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/stocks")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("warehouseId", warehouseId.toString())
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantity").value(20));
    }

    @Test
    void duplicateReferenceIsRejectedAndViewerCannotPurchase() throws Exception {
        mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + purchasingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("PO-DUPLICATE")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + purchasingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("PO-DUPLICATE")))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson("PO-FORBIDDEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void predefinedCatalogIsAvailableToAuthenticatedUsers() throws Exception {
        mvc.perform(get("/api/catalog/products")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/catalog/suppliers")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/catalog/warehouses")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    private String purchaseJson(String reference) {
        return """
                {
                  "reference": "%s",
                  "supplierId": %d,
                  "warehouseId": %d,
                  "items": [
                    {"productId": %d, "quantity": 10, "unitCost": 12.50}
                  ]
                }
                """.formatted(reference, supplierId, warehouseId, productId);
    }
}
