package com.retail.inventory.inventory_service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record InventoryItemDto(
        Long id,
        @JsonProperty("product_id") Long productId,
        int quantity,
        String location,
        @JsonProperty("last_updated") LocalDateTime lastUpdated) {
    public static InventoryItemDto fromEntity(InventoryItem item) {
        return new InventoryItemDto(item.getId(), item.getProduct().getId(), item.getQuantity(), item.getLocation(), item.getLastUpdated());
    }

    public static List<InventoryItemDto> fromEntityList(List<InventoryItem> items) {
        List<InventoryItemDto> dtoList = new ArrayList<>();

        for (InventoryItem item : items) {
            dtoList.add(fromEntity(item));
        }

        return dtoList;
    }
}

