package com.retail.inventory.inventory_service.api.controller;

import com.retail.inventory.inventory_service.api.dto.response.ProductDto;
import com.retail.inventory.inventory_service.api.dto.request.ProductPriceRequest;
import com.retail.inventory.inventory_service.api.dto.request.ProductRequestDto;
import com.retail.inventory.inventory_service.application.service.ProductService;
import com.retail.inventory.inventory_service.domain.model.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * implement application to server connection with "products" table in postgres db
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductDto> addProduct(@RequestBody ProductRequestDto req) {
        Product product = service.addProduct(req);

        // POST request needs to return HTTP 201 status if product was successfully created
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ProductDto.fromEntity(product));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable String sku) {
        Product product = service.getProduct(sku);

        // ok() returns HTTP status 200
        return ResponseEntity.ok(ProductDto.fromEntity(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<Product> products = service.getAllProducts();

        return ResponseEntity.ok(ProductDto.fromEntityList(products));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ProductDto> updatePrice(@PathVariable Long id, @RequestBody ProductPriceRequest req) {
        Product product = service.updatePrice(id, req.price());

        return ResponseEntity.ok(ProductDto.fromEntity(product));
    }
}
