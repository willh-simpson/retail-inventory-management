package com.retail.inventory.order_service.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OrderRequest(
        @JsonProperty("items") List<OrderItemRequest> items
) {
}
