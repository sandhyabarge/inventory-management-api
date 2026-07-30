package com.portfolio.inventory.purchase;

import static com.portfolio.inventory.purchase.PurchaseDtos.*;

import com.portfolio.inventory.catalog.*;
import com.portfolio.inventory.common.*;
import com.portfolio.inventory.stock.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
    private final StockPurchaseRepository purchases;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;
    private final InventoryStockRepository stocks;

    public PurchaseService(StockPurchaseRepository purchases, ProductRepository products,
            SupplierRepository suppliers, WarehouseRepository warehouses,
            InventoryStockRepository stocks) {
        this.purchases = purchases;
        this.products = products;
        this.suppliers = suppliers;
        this.warehouses = warehouses;
        this.stocks = stocks;
    }

    @Transactional
    public PurchaseResponse create(CreatePurchaseRequest request, String userEmail) {
        String reference = request.reference().trim();
        if (purchases.existsByReferenceIgnoreCase(reference)) {
            throw new ConflictException("Purchase reference already exists");
        }
        long uniqueProducts = request.items().stream()
                .map(PurchaseItemRequest::productId).distinct().count();
        if (uniqueProducts != request.items().size()) {
            throw new BadRequestException("A product may appear only once per purchase");
        }

        Supplier supplier = suppliers.findById(request.supplierId())
                .filter(Supplier::isActive)
                .orElseThrow(() -> new NotFoundException("Active supplier not found"));
        Warehouse warehouse = warehouses.findById(request.warehouseId())
                .filter(Warehouse::isActive)
                .orElseThrow(() -> new NotFoundException("Active warehouse not found"));
        StockPurchase purchase = new StockPurchase(reference, supplier, warehouse, userEmail);

        for (PurchaseItemRequest requestedItem : request.items()) {
            Product product = products.findById(requestedItem.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new NotFoundException(
                            "Active product not found: " + requestedItem.productId()));
            purchase.addItem(new StockPurchaseItem(
                    purchase, product, requestedItem.quantity(), requestedItem.unitCost()));
            InventoryStock stock = stocks
                    .findForUpdate(warehouse.getId(), product.getId())
                    .orElseGet(() -> new InventoryStock(warehouse, product));
            try {
                stock.add(requestedItem.quantity());
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Stock quantity is too large");
            }
            stocks.save(stock);
        }

        try {
            return PurchaseResponse.from(purchases.saveAndFlush(purchase));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Purchase reference already exists");
        }
    }
}
