package com.retail.inventory.order_service.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderItem;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        @JsonProperty("customer_id") String customerId,
        double total,
        OrderStatus status,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("items") List<OrderItem> items
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotal(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems()
        );
    }
}
