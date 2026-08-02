package com.portfolio.inventory.purchase;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {
    List<StockReceipt> findAllByPurchaseOrderIdOrderByReceivedAtAsc(Long purchaseOrderId);
}
