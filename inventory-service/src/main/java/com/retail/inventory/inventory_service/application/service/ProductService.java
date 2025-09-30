package com.retail.inventory.inventory_service.application.service;

import com.retail.inventory.inventory_service.api.dto.ProductRequestDto;
import com.retail.inventory.inventory_service.api.dto.ProductSnapshot;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import com.retail.inventory.inventory_service.infrastructure.messaging.ProductEventProducer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * implement application to server connection with "products" table in postgres db
 */
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    private final ProductEventProducer eventProducer;
    private final MeterRegistry meterRegistry;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo, ProductEventProducer eventProducer, MeterRegistry meterRegistry) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.eventProducer = eventProducer;
        this.meterRegistry = meterRegistry;
    }

    private Product fromRequestDto(ProductRequestDto req) {
        Category category = categoryRepo
                .findById(req.categoryId())
                .orElseThrow(() -> {
                    meterRegistry.counter("products.errors", "type", "category_not_found").increment();
                    return new RuntimeException("Category not found");
                });

        return new Product(req.id(), req.sku(), req.name(), req.description(), req.price(), category);
    }

    /**
     * return a specific product via SKU in "products" table
     *
     * @param sku product SKU number
     * @return matching product or throw error if not found
     */
    public Product getProduct(String sku) {
        long start = System.nanoTime();

        try {
            Product product = productRepo
                    .findBySku(sku)
                    .orElseThrow(() -> {
                        meterRegistry.counter("products.errors", "type", "not_found").increment();
                        return new RuntimeException("Product not found");
                    });

            meterRegistry.counter("products.found").increment();

            return product;
        } finally {
            long duration = System.nanoTime() - start;
            meterRegistry.timer("products.lookup.time").record(duration, TimeUnit.NANOSECONDS);
        }
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
        Product saved = productRepo.save(product);

        // increment counter when a new product is added
        meterRegistry.counter("products.added").increment();

        ProductSnapshot snapshot = new ProductSnapshot(
                saved.getSku(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                Map.of("category", saved.getCategory().getName())
        );

        eventProducer.publish(snapshot);

        return saved;
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
                .findById(id)
                .orElseThrow(() -> {
                    meterRegistry.counter("products.errors", "type", "not_found").increment();
                    return new RuntimeException("Product not found");
                });

        product.setPrice(price);
        meterRegistry.counter("products.price_updates").increment();

        return productRepo.save(product);
    }

    // integration with order_service
    @Cacheable(value = "product-snapshot", key = "#sku")
    public ProductSnapshot getProductBySku(String sku) {
        return productRepo.findBySku(sku)
                .map(p -> {
                    meterRegistry.counter("products.snapshot_requests").increment();
                    return new ProductSnapshot(
                            p.getSku(),
                            p.getName(),
                            p.getDescription(),
                            p.getPrice(),
                            Map.of("category", p.getCategory().getName())
                    );
                })
                .orElseThrow(() -> {
                    meterRegistry.counter("products.errors", "type", "snapshot_not_found").increment();
                    return new RuntimeException("Product not found");
                });
    }
}
