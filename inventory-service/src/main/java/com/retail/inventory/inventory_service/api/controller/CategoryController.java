package com.retail.inventory.inventory_service.api.controller;

import com.retail.inventory.inventory_service.api.dto.CategoryDto;
import com.retail.inventory.inventory_service.api.dto.CategoryNameRequest;
import com.retail.inventory.inventory_service.api.dto.CategoryRequestDto;
import com.retail.inventory.inventory_service.application.service.CategoryService;
import com.retail.inventory.inventory_service.domain.model.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * implement application to server connection with "categories" table in postgres db
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoryDto> addCategory(@RequestBody CategoryRequestDto req) {
        Category category = service.addCategory(req);

        // POST request needs to return HTTP status 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CategoryDto.fromEntity(category));
    }

    @GetMapping("/{name}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable String name) {
        Category category = service.getCategory(name);

        // ok() returns HTTP status 200
        return ResponseEntity.ok(CategoryDto.fromEntity(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<Category> categories = service.getAllCategories();

        return ResponseEntity.ok(CategoryDto.fromEntityList(categories));
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<CategoryDto> changeName(@PathVariable Long id, @RequestBody CategoryNameRequest req) {
        Category category = service.changeName(id, req.name());

        return ResponseEntity.ok(CategoryDto.fromEntity(category));
    }
}
