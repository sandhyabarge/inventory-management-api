package com.portfolio.inventory.stock;

import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final InventoryStockRepository stocks;

    public StockController(InventoryStockRepository stocks) {
        this.stocks = stocks;
    }

    @GetMapping
    @Operation(summary = "List available stock",
            description = "Returns positive stock balances, optionally filtered by warehouse or product")
    public Page<StockResponse> list(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return stocks.findAvailable(warehouseId, productId, pageable).map(StockResponse::from);
    }

    public record StockResponse(
            Long warehouseId, String warehouseCode, String warehouseName,
            Long productId, String sku, String productName,
            long quantity, Instant updatedAt) {
        static StockResponse from(InventoryStock stock) {
            return new StockResponse(
                    stock.getWarehouse().getId(), stock.getWarehouse().getCode(),
                    stock.getWarehouse().getName(), stock.getProduct().getId(),
                    stock.getProduct().getSku(), stock.getProduct().getName(),
                    stock.getQuantity(), stock.getUpdatedAt());
        }
    }
}
