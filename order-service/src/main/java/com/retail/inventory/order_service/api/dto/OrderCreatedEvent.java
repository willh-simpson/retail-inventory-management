package com.retail.inventory.order_service.api.dto;

import com.retail.inventory.order_service.domain.model.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        List<OrderItem> items,
        double total,
        LocalDateTime createdAt
) {
}
