package com.retail.inventory.order_service.api.dto.request;

public record OrderItemRequest(
        String sku,
        int quantity
) {
}
