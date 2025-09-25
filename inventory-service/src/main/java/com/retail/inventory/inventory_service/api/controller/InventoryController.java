package com.retail.inventory.inventory_service.api.controller;

import com.retail.inventory.inventory_service.domain.model.Inventory;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.InventoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * implement application to server connection with "inventory" table in postgres db
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryRepository repo;

    public InventoryController(InventoryRepository repo) {
        this.repo = repo;
    }

    /**
     * return inventory information for a specific product
     *
     * @param product product to request inventory information
     * @return matching inventory or throw error if not found
     */
    public Inventory getInventory(Product product) {
        return repo
                .findByProduct(product)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    }

    /**
     * return all inventory information for each product
     *
     * @return list of all inventory for all mapped products
     */
    @GetMapping
    public List<Inventory> getAllInventory() {
        return repo.findAll();
    }

    /**
     * add inventory information for a product
     *
     * @param inv inventory information to add
     * @return inventory upon success
     */
    @PostMapping
    public Inventory addInventory(@RequestBody Inventory inv) {
        return repo.save(inv);
    }
}
