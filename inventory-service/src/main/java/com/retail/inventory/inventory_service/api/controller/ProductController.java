package com.retail.inventory.inventory_service.api.controller;

import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * implement application to server connection with "products" table in postgres db
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    /**
     * return all products in "products" table
     *
     * @return list of all mapped products
     */
    @GetMapping
    public List<Product> getAllProducts() {
        return repo.findAll();
    }

    /**
     * add individual product to "products" table
     *
     * @param product product with all relevant information to send to server
     * @return product upon success
     */
    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return repo.save(product);
    }
}
