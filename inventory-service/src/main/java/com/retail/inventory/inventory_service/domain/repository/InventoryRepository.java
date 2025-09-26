package com.retail.inventory.inventory_service.domain.repository;

import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * commands to connect with "inventory" table in postgres db
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByProductId(Long productId);
}
