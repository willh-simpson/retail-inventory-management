package com.retail.inventory.order_service.api.dto.response;

import com.retail.inventory.common.messaging.InventoryReservationStatus;

public record ReserveResponse(
        boolean success,
        InventoryReservationStatus status,
        String message
) {
    public static ReserveResponse systemError(String message) {
        return new ReserveResponse(
                false,
                InventoryReservationStatus.ERROR,
                message
        );
    }
}
