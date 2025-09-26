package com.retail.inventory.inventory_service.application.service;

import com.retail.inventory.inventory_service.api.dto.ProductRequestDto;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * implement application to server connection with "products" table in postgres db
 */
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    private Product fromRequestDto(ProductRequestDto req) {
        Category category = categoryRepo
                .findByCategoryId(req.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return new Product(req.id(), req.sku(), req.name(), req.description(), req.price(), category);
    }

    /**
     * return a specific product via SKU in "products" table
     *
     * @param sku product SKU number
     * @return matching product or throw error if not found
     */
    public Product getProduct(String sku) {
        return productRepo
                .findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    /**
     * return all products in "products" table
     *
     * @return list of all mapped products
     */
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    /**
     * add product to "products" table
     *
     * @param req product to add
     * @return product upon success
     */
    public Product addProduct(ProductRequestDto req) {
        Product product = fromRequestDto(req);

        return productRepo.save(product);
    }

    /**
     * change a product's price
     *
     * @param id product id
     * @param price new price
     * @return product upon success
     */
    public Product updatePrice(Long id, double price) {
        Product product = productRepo
                .findByProductId(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setPrice(price);

        return productRepo.save(product);
    }
}
