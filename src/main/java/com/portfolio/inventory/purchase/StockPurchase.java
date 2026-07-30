package com.portfolio.inventory.purchase;

import com.portfolio.inventory.catalog.Supplier;
import com.portfolio.inventory.catalog.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "stock_purchases")
public class StockPurchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String reference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;
    @Column(name = "created_by_email", nullable = false, length = 320)
    private String createdByEmail;
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockPurchaseItem> items = new ArrayList<>();

    protected StockPurchase() {}

    public StockPurchase(String reference, Supplier supplier, Warehouse warehouse,
            String createdByEmail) {
        this.reference = reference;
        this.supplier = supplier;
        this.warehouse = warehouse;
        this.createdByEmail = createdByEmail;
        this.purchasedAt = Instant.now();
        this.totalCost = BigDecimal.ZERO;
    }

    public void addItem(StockPurchaseItem item) {
        items.add(item);
        totalCost = totalCost.add(item.lineTotal());
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Supplier getSupplier() { return supplier; }
    public Warehouse getWarehouse() { return warehouse; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public String getCreatedByEmail() { return createdByEmail; }
    public BigDecimal getTotalCost() { return totalCost; }
    public List<StockPurchaseItem> getItems() { return List.copyOf(items); }
}
