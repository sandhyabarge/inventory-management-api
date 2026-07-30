package com.portfolio.inventory.purchase;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {
    boolean existsByReferenceIgnoreCase(String reference);
}
