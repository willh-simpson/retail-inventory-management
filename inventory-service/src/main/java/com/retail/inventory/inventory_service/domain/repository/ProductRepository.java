package com.retail.inventory.inventory_service.domain.repository;

import com.retail.inventory.inventory_service.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * commands to connect with "products" table in postgres db
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findByProductId(Long id);
}
