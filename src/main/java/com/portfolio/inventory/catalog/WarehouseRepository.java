package com.portfolio.inventory.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findAllByActiveTrueOrderByName();
}
