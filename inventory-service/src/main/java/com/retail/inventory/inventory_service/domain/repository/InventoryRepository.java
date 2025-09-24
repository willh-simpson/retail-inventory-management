package com.retail.inventory.inventory_service.domain.repository;

import com.retail.inventory.inventory_service.domain.model.Inventory;
import com.retail.inventory.inventory_service.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * commands to connect with "inventory" table in postgres db
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProduct(Product product);
}
