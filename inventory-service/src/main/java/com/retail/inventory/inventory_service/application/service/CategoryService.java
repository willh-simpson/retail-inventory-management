package com.retail.inventory.inventory_service.application.service;

import com.retail.inventory.inventory_service.api.dto.request.CategoryRequestDto;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * directly connect with repo and server connection for "categories" table
 */
@Service
public class CategoryService {
    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    private Category fromRequestDto(CategoryRequestDto req) {
        return new Category(req.id(), req.name(), req.description());
    }

    public Category getCategory(String name) {
        return repo
                .findByName(name)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public List<Category> getAllCategories() {
        return repo.findAll();
    }

    public Category addCategory(CategoryRequestDto req) {
        Category category = fromRequestDto(req);

        return repo.save(category);
    }

    public Category changeName(Long id, String name) {
        Category category = repo
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(name);

        return repo.save(category);
    }
}
