package com.portfolio.inventory.purchase;

import static com.portfolio.inventory.purchase.PurchaseDtos.*;

import com.portfolio.inventory.catalog.*;
import com.portfolio.inventory.common.*;
import com.portfolio.inventory.stock.*;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
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
        requireUniqueProductIds(request.items().stream().map(PurchaseItemRequest::productId).toList());
        Supplier supplier = suppliers.findById(request.supplierId()).filter(Supplier::isActive)
                .orElseThrow(() -> new NotFoundException("Active supplier not found"));
        Warehouse warehouse = warehouses.findById(request.warehouseId()).filter(Warehouse::isActive)
                .orElseThrow(() -> new NotFoundException("Active warehouse not found"));
        StockPurchase purchase = new StockPurchase(reference, supplier, warehouse, userEmail);
        for (PurchaseItemRequest item : request.items()) {
            Product product = products.findById(item.productId()).filter(Product::isActive)
                    .orElseThrow(() -> new NotFoundException("Active product not found: " + item.productId()));
            purchase.addItem(new StockPurchaseItem(purchase, product, item.quantity(), item.unitCost()));
        }
        try {
            return PurchaseResponse.from(purchases.saveAndFlush(purchase));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Purchase reference already exists");
        }
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(Long id) {
        return PurchaseResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> list(PurchaseStatus status, Pageable pageable) {
        Page<StockPurchase> page = status == null ? purchases.findAll(pageable)
                : purchases.findAllByStatus(status, pageable);
        return page.map(PurchaseResponse::from);
    }

    @Transactional
    public PurchaseResponse submit(Long id) {
        return transition(id, StockPurchase::submit);
    }

    @Transactional
    public PurchaseResponse approve(Long id, String approverEmail) {
        return transition(id, purchase -> purchase.approve(approverEmail));
    }

    @Transactional
    public PurchaseResponse cancel(Long id) {
        return transition(id, StockPurchase::cancel);
    }

    @Transactional
    public PurchaseResponse receive(Long id, ReceivePurchaseRequest request) {
        requireUniqueProductIds(request.items().stream().map(ReceiveItemRequest::productId).toList());
        StockPurchase purchase = findForUpdate(id);
        if (purchase.getStatus() != PurchaseStatus.APPROVED
                && purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("Stock can be received only for an approved purchase order");
        }
        Map<Long, StockPurchaseItem> orderedItems = new HashMap<>();
        purchase.getItems().forEach(item -> orderedItems.put(item.getProduct().getId(), item));
        for (ReceiveItemRequest received : request.items()) {
            StockPurchaseItem item = orderedItems.get(received.productId());
            if (item == null) {
                throw new BadRequestException("Product is not part of this purchase order: " + received.productId());
            }
            try {
                item.receive(received.quantity());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(ex.getMessage() + ": " + received.productId());
            }
            InventoryStock stock = stocks.findForUpdate(
                            purchase.getWarehouse().getId(), received.productId())
                    .orElseGet(() -> new InventoryStock(purchase.getWarehouse(), item.getProduct()));
            try {
                stock.add(received.quantity());
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Stock quantity is too large");
            }
            stocks.save(stock);
        }
        purchase.refreshReceiptStatus();
        return PurchaseResponse.from(purchase);
    }

    private PurchaseResponse transition(Long id, Consumer<StockPurchase> action) {
        StockPurchase purchase = findForUpdate(id);
        try {
            action.accept(purchase);
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        return PurchaseResponse.from(purchase);
    }

    private StockPurchase find(Long id) {
        return purchases.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private StockPurchase findForUpdate(Long id) {
        return purchases.findForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private void requireUniqueProductIds(List<Long> ids) {
        if (ids.stream().distinct().count() != ids.size()) {
            throw new BadRequestException("A product may appear only once in the request");
        }
    }
}
