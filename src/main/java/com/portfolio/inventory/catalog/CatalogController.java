package com.portfolio.inventory.catalog;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;

    public CatalogController(ProductRepository products, SupplierRepository suppliers,
            WarehouseRepository warehouses) {
        this.products = products;
        this.suppliers = suppliers;
        this.warehouses = warehouses;
    }

    @GetMapping("/products")
    @Operation(summary = "List the five predefined products")
    public List<ProductResponse> products() {
        return products.findAllByActiveTrueOrderByName().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/suppliers")
    @Operation(summary = "List the three predefined suppliers")
    public List<SupplierResponse> suppliers() {
        return suppliers.findAllByActiveTrueOrderByName().stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/warehouses")
    @Operation(summary = "List the three predefined warehouses")
    public List<WarehouseResponse> warehouses() {
        return warehouses.findAllByActiveTrueOrderByName().stream().map(WarehouseResponse::from).toList();
    }

    public record ProductResponse(Long id, String sku, String name) {
        static ProductResponse from(Product value) {
            return new ProductResponse(value.getId(), value.getSku(), value.getName());
        }
    }
    public record SupplierResponse(Long id, String code, String name) {
        static SupplierResponse from(Supplier value) {
            return new SupplierResponse(value.getId(), value.getCode(), value.getName());
        }
    }
    public record WarehouseResponse(Long id, String code, String name) {
        static WarehouseResponse from(Warehouse value) {
            return new WarehouseResponse(value.getId(), value.getCode(), value.getName());
        }
    }
}
