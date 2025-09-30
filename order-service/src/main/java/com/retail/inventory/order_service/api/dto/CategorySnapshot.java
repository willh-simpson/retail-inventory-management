package com.retail.inventory.order_service.api.dto;

import com.retail.inventory.order_service.domain.model.CategorySnapshotEntity;

public record CategorySnapshot(
        String name,
        String description
) {
    public static CategorySnapshotEntity toEntity(CategorySnapshot snapshot, Long version) {
        return new CategorySnapshotEntity(
                snapshot.name(),
                snapshot.description(),
                version
        );
    }
}
