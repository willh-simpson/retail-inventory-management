package com.retail.inventory.inventory_service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.inventory.inventory_service.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public record ProductDto(
        Long id,
        String sku,
        String name,
        String description,
        double price,
        @JsonProperty("category_id") Long categoryId) {
    public static ProductDto fromEntity(Product product) {
        return new ProductDto(product.getId(), product.getSku(), product.getName(), product.getDescription(), product.getPrice(), product.getCategory().getId());
    }

    public static List<ProductDto> fromEntityList(List<Product> products) {
        List<ProductDto> dtoList = new ArrayList<>();

        for (Product product : products) {
            dtoList.add(fromEntity(product));
        }

        return dtoList;
    }
}
