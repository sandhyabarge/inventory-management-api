package com.portfolio.inventory.stock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select stock from InventoryStock stock
            where stock.warehouse.id = :warehouseId and stock.product.id = :productId
            """)
    Optional<InventoryStock> findForUpdate(
            @Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

    @Query("""
            select stock from InventoryStock stock
            join fetch stock.warehouse warehouse
            join fetch stock.product product
            where stock.quantity > 0
              and (:warehouseId is null or warehouse.id = :warehouseId)
              and (:productId is null or product.id = :productId)
            """)
    Page<InventoryStock> findAvailable(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            Pageable pageable);
}
