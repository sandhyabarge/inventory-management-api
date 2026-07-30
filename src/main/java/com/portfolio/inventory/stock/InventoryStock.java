package com.portfolio.inventory.stock;

import com.portfolio.inventory.catalog.Product;
import com.portfolio.inventory.catalog.Warehouse;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_stocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id"}))
public class InventoryStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private long quantity;
    @Version
    private long version;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryStock() {}

    public InventoryStock(Warehouse warehouse, Product product) {
        this.warehouse = warehouse;
        this.product = product;
        this.updatedAt = Instant.now();
    }

    public void add(long amount) {
        quantity = Math.addExact(quantity, amount);
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Warehouse getWarehouse() { return warehouse; }
    public Product getProduct() { return product; }
    public long getQuantity() { return quantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}
