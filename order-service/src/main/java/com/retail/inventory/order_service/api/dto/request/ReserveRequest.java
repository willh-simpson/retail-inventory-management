package com.retail.inventory.order_service.api.dto.request;

import com.retail.inventory.order_service.domain.model.order.OrderItem;

import java.util.List;

public record ReserveRequest(
        List<OrderItem> items
) {
}
