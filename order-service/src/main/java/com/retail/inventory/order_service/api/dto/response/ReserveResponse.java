package com.retail.inventory.order_service.api.dto.response;

public record ReserveResponse(
        boolean success,
        String message
) {
}
