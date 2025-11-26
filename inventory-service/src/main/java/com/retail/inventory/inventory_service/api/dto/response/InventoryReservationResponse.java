package com.retail.inventory.inventory_service.api.dto.response;

import com.retail.inventory.common.messaging.InventoryReservationStatus;

public record InventoryReservationResponse(
        boolean success,
        InventoryReservationStatus status,
        String message
) {
}
