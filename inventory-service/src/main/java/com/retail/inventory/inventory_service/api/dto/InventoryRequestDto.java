package com.retail.inventory.inventory_service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record InventoryRequestDto(
        Long id,
        @JsonProperty("product_id") Long productId,
        int quantity,
        String location,
        @JsonProperty("last_updated") LocalDateTime lastUpdated) {
}
