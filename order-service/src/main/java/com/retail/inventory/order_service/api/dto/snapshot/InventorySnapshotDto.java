package com.retail.inventory.order_service.api.dto.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshot;

import java.time.LocalDateTime;

public record InventorySnapshotDto(
        String productSku,
        int quantity,
        String location,
        LocalDateTime lastUpdated
) {
    public static InventorySnapshot toEntity(InventorySnapshotDto snapshot, Long version) {
        return new InventorySnapshot(
                snapshot.productSku(),
                snapshot.quantity(),
                snapshot.location(),
                snapshot.lastUpdated(),
                version
        );
    }
}
