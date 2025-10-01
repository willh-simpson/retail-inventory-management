package com.retail.inventory.inventory_service.api.dto.request;

public record InventoryReservationRequest(
        Long productId,
        int quantity,
        Long orderId
) {
}
