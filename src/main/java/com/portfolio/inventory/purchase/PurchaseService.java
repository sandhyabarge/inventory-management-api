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
    private final PurchaseOrderRepository purchases;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;
    private final InventoryStockRepository stocks;
    private final StockReceiptRepository receipts;

    public PurchaseService(PurchaseOrderRepository purchases, ProductRepository products,
            SupplierRepository suppliers, WarehouseRepository warehouses,
            InventoryStockRepository stocks, StockReceiptRepository receipts) {
        this.purchases = purchases;
        this.products = products;
        this.suppliers = suppliers;
        this.warehouses = warehouses;
        this.stocks = stocks;
        this.receipts = receipts;
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
        PurchaseOrder purchase = new PurchaseOrder(reference, supplier, warehouse, userEmail);
        for (PurchaseItemRequest item : request.items()) {
            Product product = products.findById(item.productId()).filter(Product::isActive)
                    .orElseThrow(() -> new NotFoundException("Active product not found: " + item.productId()));
            purchase.addItem(new PurchaseOrderItem(purchase, product, item.quantity(), item.unitCost()));
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
        Page<PurchaseOrder> page = status == null ? purchases.findAll(pageable)
                : purchases.findAllByStatus(status, pageable);
        return page.map(PurchaseResponse::from);
    }

    @Transactional
    public PurchaseResponse submit(Long id) {
        return transition(id, PurchaseOrder::submit);
    }

    @Transactional
    public PurchaseResponse updateDraft(Long id, UpdateDraftRequest request) {
        requireUniqueProductIds(request.items().stream().map(PurchaseItemRequest::productId).toList());
        PurchaseOrder order = findForUpdate(id);
        if (order.getStatus() != PurchaseStatus.DRAFT) {
            throw new ConflictException("Only a draft purchase order can be edited");
        }
        Supplier supplier = suppliers.findById(request.supplierId()).filter(Supplier::isActive)
                .orElseThrow(() -> new NotFoundException("Active supplier not found"));
        Warehouse warehouse = warehouses.findById(request.warehouseId()).filter(Warehouse::isActive)
                .orElseThrow(() -> new NotFoundException("Active warehouse not found"));
        List<PurchaseOrderItem> items = request.items().stream().map(item -> {
            Product product = products.findById(item.productId()).filter(Product::isActive)
                    .orElseThrow(() -> new NotFoundException("Active product not found: " + item.productId()));
            return new PurchaseOrderItem(order, product, item.quantity(), item.unitCost());
        }).toList();
        order.replaceDraftDetails(supplier, warehouse, items);
        return PurchaseResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> receipts(Long id) {
        find(id);
        return receipts.findAllByPurchaseOrderIdOrderByReceivedAtAsc(id).stream()
                .map(ReceiptResponse::from).toList();
    }

    @Transactional
    public PurchaseResponse approve(Long id, String approverEmail) {
        return transition(id, purchase -> purchase.approve(approverEmail));
    }

    @Transactional
    public PurchaseResponse cancel(Long id) {
        return transition(id, PurchaseOrder::cancel);
    }

    @Transactional
    public PurchaseResponse receive(Long id, ReceivePurchaseRequest request, String receiverEmail) {
        requireUniqueProductIds(request.items().stream().map(ReceiveItemRequest::productId).toList());
        PurchaseOrder purchase = findForUpdate(id);
        if (purchase.getStatus() != PurchaseStatus.APPROVED
                && purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("Stock can be received only for an approved purchase order");
        }
        Map<Long, PurchaseOrderItem> orderedItems = new HashMap<>();
        purchase.getItems().forEach(item -> orderedItems.put(item.getProduct().getId(), item));
        for (ReceiveItemRequest received : request.items()) {
            PurchaseOrderItem item = orderedItems.get(received.productId());
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
            receipts.save(new StockReceipt(purchase, item.getProduct(),
                    received.quantity(), receiverEmail));
        }
        purchase.refreshReceiptStatus();
        return PurchaseResponse.from(purchase);
    }

    private PurchaseResponse transition(Long id, Consumer<PurchaseOrder> action) {
        PurchaseOrder purchase = findForUpdate(id);
        try {
            action.accept(purchase);
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        return PurchaseResponse.from(purchase);
    }

    private PurchaseOrder find(Long id) {
        return purchases.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private PurchaseOrder findForUpdate(Long id) {
        return purchases.findForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private void requireUniqueProductIds(List<Long> ids) {
        if (ids.stream().distinct().count() != ids.size()) {
            throw new BadRequestException("A product may appear only once in the request");
        }
    }
}
