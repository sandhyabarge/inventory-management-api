package com.portfolio.inventory.purchase;

import com.portfolio.inventory.catalog.Product;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_receipts")
public class StockReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private long quantity;
    @Column(name = "received_by_email", nullable = false, length = 320)
    private String receivedByEmail;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected StockReceipt() {}
    public StockReceipt(PurchaseOrder order, Product product, long quantity, String email) {
        this.purchaseOrder = order; this.product = product; this.quantity = quantity;
        this.receivedByEmail = email; this.receivedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public long getQuantity() { return quantity; }
    public String getReceivedByEmail() { return receivedByEmail; }
    public Instant getReceivedAt() { return receivedAt; }
}
