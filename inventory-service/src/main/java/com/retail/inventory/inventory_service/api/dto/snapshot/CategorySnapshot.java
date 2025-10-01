package com.retail.inventory.inventory_service.api.dto.snapshot;

import com.retail.inventory.inventory_service.domain.model.Category;

public record CategorySnapshot(
        String name,
        String description
) {
    public CategorySnapshot fromEntity(Category category) {
        return new CategorySnapshot(category.getName(), category.getDescription());
    }
}
