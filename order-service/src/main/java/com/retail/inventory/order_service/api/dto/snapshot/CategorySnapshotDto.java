package com.retail.inventory.order_service.api.dto.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshot;

public record CategorySnapshotDto(
        String name,
        String description
) {
    public static CategorySnapshot toEntity(CategorySnapshotDto snapshot, Long version) {
        return new CategorySnapshot(
                snapshot.name(),
                snapshot.description(),
                version
        );
    }
}
