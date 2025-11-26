package com.retail.inventory.order_service.api.controller;

import com.retail.inventory.order_service.api.dto.request.OrderRequest;
import com.retail.inventory.order_service.api.dto.response.OrderResponse;
import com.retail.inventory.order_service.application.service.OrderService;
import com.retail.inventory.order_service.domain.model.order.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest req) {
        Order order = service.createOrder(req);

        return switch (order.getStatus()) {
            case CONFIRMED -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(OrderResponse.fromEntity(order));

            case PENDING_RETRY -> ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(OrderResponse.fromEntity(order));

            default -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OrderResponse.fromEntity(order));
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        Order order = service.getById(id);

        return ResponseEntity.ok(OrderResponse.fromEntity(order));
    }
}
