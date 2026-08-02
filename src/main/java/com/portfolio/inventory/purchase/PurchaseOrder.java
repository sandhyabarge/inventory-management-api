package com.portfolio.inventory.purchase;

import com.portfolio.inventory.catalog.Supplier;
import com.portfolio.inventory.catalog.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "stock_purchases")
public class PurchaseOrder {
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseStatus status;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "approved_by_email", length = 320)
    private String approvedByEmail;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {}

    public PurchaseOrder(String reference, Supplier supplier, Warehouse warehouse,
            String createdByEmail) {
        this.reference = reference;
        this.supplier = supplier;
        this.warehouse = warehouse;
        this.createdByEmail = createdByEmail;
        this.purchasedAt = Instant.now();
        this.totalCost = BigDecimal.ZERO;
        this.status = PurchaseStatus.DRAFT;
    }

    public void addItem(PurchaseOrderItem item) {
        items.add(item);
        totalCost = totalCost.add(item.lineTotal());
    }

    public void replaceDraftDetails(Supplier supplier, Warehouse warehouse,
            List<PurchaseOrderItem> replacementItems) {
        requireStatus(PurchaseStatus.DRAFT);
        this.supplier = supplier;
        this.warehouse = warehouse;
        items.clear();
        totalCost = BigDecimal.ZERO;
        replacementItems.forEach(this::addItem);
    }

    public void submit() {
        requireStatus(PurchaseStatus.DRAFT);
        status = PurchaseStatus.SUBMITTED;
        submittedAt = Instant.now();
    }

    public void approve(String approverEmail) {
        requireStatus(PurchaseStatus.SUBMITTED);
        status = PurchaseStatus.APPROVED;
        approvedAt = Instant.now();
        approvedByEmail = approverEmail;
    }

    public void refreshReceiptStatus() {
        boolean allReceived = items.stream().allMatch(PurchaseOrderItem::isFullyReceived);
        boolean anyReceived = items.stream().anyMatch(item -> item.getReceivedQuantity() > 0);
        status = allReceived ? PurchaseStatus.RECEIVED
                : anyReceived ? PurchaseStatus.PARTIALLY_RECEIVED : PurchaseStatus.APPROVED;
    }

    public void cancel() {
        if (status != PurchaseStatus.DRAFT && status != PurchaseStatus.SUBMITTED
                && status != PurchaseStatus.APPROVED) {
            throw new IllegalStateException("Purchase order cannot be cancelled from " + status);
        }
        status = PurchaseStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    private void requireStatus(PurchaseStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected purchase order status " + expected + " but was " + status);
        }
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Supplier getSupplier() { return supplier; }
    public Warehouse getWarehouse() { return warehouse; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public String getCreatedByEmail() { return createdByEmail; }
    public BigDecimal getTotalCost() { return totalCost; }
    public List<PurchaseOrderItem> getItems() { return List.copyOf(items); }
    public PurchaseStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getApprovedByEmail() { return approvedByEmail; }
    public Instant getCancelledAt() { return cancelledAt; }
}
