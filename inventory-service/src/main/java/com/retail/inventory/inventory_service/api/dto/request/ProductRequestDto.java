package com.retail.inventory.inventory_service.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductRequestDto(Long id, String sku, String name, String description, double price, @JsonProperty("category_id") Long categoryId) {
}
