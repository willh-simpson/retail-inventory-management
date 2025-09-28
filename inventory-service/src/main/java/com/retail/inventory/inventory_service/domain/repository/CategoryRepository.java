package com.retail.inventory.inventory_service.domain.repository;

import com.retail.inventory.inventory_service.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * commands to connect with "categories" table in postgres db
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
}
