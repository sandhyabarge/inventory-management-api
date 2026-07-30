package com.portfolio.inventory.purchase;

import com.portfolio.inventory.catalog.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_purchase_items")
public class StockPurchaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private StockPurchase purchase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private long quantity;
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    protected StockPurchaseItem() {}

    public StockPurchaseItem(StockPurchase purchase, Product product, long quantity,
            BigDecimal unitCost) {
        this.purchase = purchase;
        this.product = product;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public BigDecimal lineTotal() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() { return product; }
    public long getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
}
