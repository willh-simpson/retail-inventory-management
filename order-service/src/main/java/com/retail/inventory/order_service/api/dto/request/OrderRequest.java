package com.retail.inventory.order_service.api.dto.request;

import java.util.List;

public record OrderRequest(
        List<OrderItemRequest> items
) {
}
