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
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
                    BigDecimal unitCost) {}

    public record PurchaseItemResponse(
            Long productId, String sku, String productName, long quantity,
            BigDecimal unitCost, BigDecimal lineTotal) {
        static PurchaseItemResponse from(StockPurchaseItem item) {
            return new PurchaseItemResponse(
                    item.getProduct().getId(), item.getProduct().getSku(),
                    item.getProduct().getName(), item.getQuantity(), item.getUnitCost(),
                    item.lineTotal());
        }
    }

    public record PurchaseResponse(
            Long id, String reference, Long supplierId, String supplierCode,
            Long warehouseId, String warehouseCode, Instant purchasedAt,
            String createdByEmail, BigDecimal totalCost, List<PurchaseItemResponse> items) {
        static PurchaseResponse from(StockPurchase purchase) {
            return new PurchaseResponse(
                    purchase.getId(), purchase.getReference(), purchase.getSupplier().getId(),
                    purchase.getSupplier().getCode(), purchase.getWarehouse().getId(),
                    purchase.getWarehouse().getCode(), purchase.getPurchasedAt(),
                    purchase.getCreatedByEmail(), purchase.getTotalCost(),
                    purchase.getItems().stream().map(PurchaseItemResponse::from).toList());
        }
    }
}
