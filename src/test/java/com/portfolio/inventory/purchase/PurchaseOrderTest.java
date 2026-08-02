package com.portfolio.inventory.purchase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.portfolio.inventory.catalog.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PurchaseOrderTest {
    @Test
    void followsHappyPathThroughPartialAndFullReceipt() {
        PurchaseOrder order = order(10);
        order.submit();
        order.approve("manager@example.com");
        order.getItems().getFirst().receive(4);
        order.refreshReceiptStatus();
        assertEquals(PurchaseStatus.PARTIALLY_RECEIVED, order.getStatus());
        order.getItems().getFirst().receive(6);
        order.refreshReceiptStatus();
        assertEquals(PurchaseStatus.RECEIVED, order.getStatus());
    }

    @Test
    void rejectsInvalidTransitionsOverReceiptAndReceivedCancellation() {
        PurchaseOrder order = order(5);
        assertThrows(IllegalStateException.class, () -> order.approve("manager@example.com"));
        order.submit();
        order.approve("manager@example.com");
        assertThrows(IllegalArgumentException.class, () -> order.getItems().getFirst().receive(6));
        order.getItems().getFirst().receive(5);
        order.refreshReceiptStatus();
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void permitsDraftEditingAndRecalculatesTotal() {
        PurchaseOrder order = order(5);
        Product product = mock(Product.class);
        order.replaceDraftDetails(mock(Supplier.class), mock(Warehouse.class),
                java.util.List.of(new PurchaseOrderItem(order, product, 3, new BigDecimal("2.00"))));
        assertEquals(new BigDecimal("6.00"), order.getTotalCost());
    }

    private PurchaseOrder order(long quantity) {
        PurchaseOrder order = new PurchaseOrder("PO-UNIT", mock(Supplier.class),
                mock(Warehouse.class), "buyer@example.com");
        order.addItem(new PurchaseOrderItem(order, mock(Product.class), quantity,
                new BigDecimal("10.00")));
        return order;
    }
}
