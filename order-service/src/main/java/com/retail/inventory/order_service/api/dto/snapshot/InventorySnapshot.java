package com.retail.inventory.order_service.api.dto.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshotEntity;

import java.time.LocalDateTime;

public record InventorySnapshot(
        String productSku,
        int quantity,
        String location,
        LocalDateTime lastUpdated
) {
    public static InventorySnapshotEntity toEntity(InventorySnapshot snapshot, Long version) {
        return new InventorySnapshotEntity(
                snapshot.productSku(),
                snapshot.quantity(),
                snapshot.location(),
                snapshot.lastUpdated(),
                version
        );
    }
}
