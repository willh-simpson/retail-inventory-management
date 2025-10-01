package com.retail.inventory.order_service.api.dto.request;

public record ReserveRequest(
        String sku,
        int quantity
) {
}
