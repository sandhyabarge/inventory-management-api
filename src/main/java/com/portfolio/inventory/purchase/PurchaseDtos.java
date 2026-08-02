package com.portfolio.inventory.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PurchaseDtos {
    private PurchaseDtos() {}

    public record CreatePurchaseRequest(
            @NotBlank @Size(max = 80) String reference,
            @NotNull @Positive Long supplierId,
            @NotNull @Positive Long warehouseId,
            @NotEmpty List<@Valid PurchaseItemRequest> items) {}

    public record PurchaseItemRequest(
            @NotNull @Positive Long productId,
            @Positive long quantity,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
                    BigDecimal unitCost) {}

    public record ReceivePurchaseRequest(
            @NotEmpty List<@Valid ReceiveItemRequest> items) {}

    public record ReceiveItemRequest(
            @NotNull @Positive Long productId,
            @Positive long quantity) {}

    public record PurchaseItemResponse(
            Long productId, String sku, String productName, long orderedQuantity,
            long receivedQuantity, long outstandingQuantity, BigDecimal unitCost,
            BigDecimal lineTotal) {
        static PurchaseItemResponse from(StockPurchaseItem item) {
            return new PurchaseItemResponse(item.getProduct().getId(), item.getProduct().getSku(),
                    item.getProduct().getName(), item.getQuantity(), item.getReceivedQuantity(),
                    item.getQuantity() - item.getReceivedQuantity(), item.getUnitCost(),
                    item.lineTotal());
        }
    }

    public record PurchaseResponse(
            Long id, String reference, PurchaseStatus status,
            Long supplierId, String supplierCode, Long warehouseId, String warehouseCode,
            Instant createdAt, Instant submittedAt, Instant approvedAt,
            String createdByEmail, String approvedByEmail, Instant cancelledAt,
            BigDecimal totalCost, List<PurchaseItemResponse> items) {
        static PurchaseResponse from(StockPurchase purchase) {
            return new PurchaseResponse(purchase.getId(), purchase.getReference(),
                    purchase.getStatus(), purchase.getSupplier().getId(),
                    purchase.getSupplier().getCode(), purchase.getWarehouse().getId(),
                    purchase.getWarehouse().getCode(), purchase.getPurchasedAt(),
                    purchase.getSubmittedAt(), purchase.getApprovedAt(),
                    purchase.getCreatedByEmail(), purchase.getApprovedByEmail(),
                    purchase.getCancelledAt(), purchase.getTotalCost(),
                    purchase.getItems().stream().map(PurchaseItemResponse::from).toList());
        }
    }
}
