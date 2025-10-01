package com.retail.inventory.inventory_service.api.dto.response;

import com.retail.inventory.inventory_service.domain.model.Category;

import java.util.ArrayList;
import java.util.List;

public record CategoryDto(Long id, String name, String description) {
    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }

    public static List<CategoryDto> fromEntityList(List<Category> categories) {
        List<CategoryDto> dtoList = new ArrayList<>();

        for (Category category : categories) {
            dtoList.add(fromEntity(category));
        }

        return dtoList;
    }
}
