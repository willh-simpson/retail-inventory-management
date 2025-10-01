package com.retail.inventory.order_service.api.dto;

import com.retail.inventory.order_service.domain.model.Order;
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
    public static OrderCreatedEvent fromEntity(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                1L,
                order.getItems(),
                order.getTotal(),
                LocalDateTime.now()
        );
    }
}
