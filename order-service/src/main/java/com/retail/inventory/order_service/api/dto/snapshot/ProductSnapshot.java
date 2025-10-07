package com.retail.inventory.order_service.api.dto.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;

import java.util.Map;

public record ProductSnapshot(
        String sku,
        String name,
        String description,
        double price,
        Map<String, Object> attributes
) {
    public static ProductSnapshotEntity toEntity(ProductSnapshot snapshot, Long version) {
        return new ProductSnapshotEntity(
                snapshot.sku(),
                snapshot.name(),
                snapshot.description(),
                snapshot.price(),
                snapshot.attributes(),
                version
        );
    }
}
