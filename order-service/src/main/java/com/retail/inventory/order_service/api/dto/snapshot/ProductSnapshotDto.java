package com.retail.inventory.order_service.api.dto.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;

import java.util.Map;

public record ProductSnapshotDto(
        String sku,
        String name,
        String description,
        double price,
        Map<String, Object> attributes
) {
    public static ProductSnapshot toEntity(ProductSnapshotDto snapshot, Long version) {
        return new ProductSnapshot(
                snapshot.sku(),
                snapshot.name(),
                snapshot.description(),
                snapshot.price(),
                snapshot.attributes(),
                version
        );
    }
}
