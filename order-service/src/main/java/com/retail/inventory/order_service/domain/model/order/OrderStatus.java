package com.retail.inventory.order_service.domain.model.order;

public enum OrderStatus {
    PENDING_RETRY,
    CONFIRMED,
    SHIPPED,
    CANCELLED,
    FAILED
}
