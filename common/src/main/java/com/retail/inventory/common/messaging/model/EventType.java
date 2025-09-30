package com.retail.inventory.common.messaging.model;

public enum EventType {
    // product events
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_PRICE_CHANGED,

    // inventory events
    INVENTORY_CREATED,
    INVENTORY_UPDATED,
    STOCK_ADDED,

    // order events
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_CANCELLED,

    // category events
    CATEGORY_CREATED,
    CATEGORY_UPDATED
}
