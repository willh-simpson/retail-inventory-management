package com.retail.inventory.inventory_service.api.dto;

import java.util.Map;

public record ProductSnapshot(
        String sku,
        String name,
        String description,
        double price,
        Map<String, Object> attributes
) {
}
