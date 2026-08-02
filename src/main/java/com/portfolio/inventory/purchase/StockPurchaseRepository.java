package com.portfolio.inventory.purchase;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {
    boolean existsByReferenceIgnoreCase(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct p from StockPurchase p left join fetch p.items where p.id = :id")
    Optional<StockPurchase> findForUpdate(@Param("id") Long id);

    Page<StockPurchase> findAllByStatus(PurchaseStatus status, Pageable pageable);
}
