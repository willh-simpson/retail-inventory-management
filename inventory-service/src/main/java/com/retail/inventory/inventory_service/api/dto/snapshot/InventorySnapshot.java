package com.retail.inventory.inventory_service.api.dto.snapshot;

import com.retail.inventory.inventory_service.domain.model.InventoryItem;

import java.time.LocalDateTime;

public record InventorySnapshot(
        Long productId,
        String productSku,
        int quantity,
        String location,
        LocalDateTime lastUpdated
) {
    public static InventorySnapshot fromEntity(InventoryItem item) {
        return new InventorySnapshot(
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getLocation(),
                item.getLastUpdated()
        );
    }
}
