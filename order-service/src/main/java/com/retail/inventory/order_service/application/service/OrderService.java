package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.request.OrderItemRequest;
import com.retail.inventory.order_service.api.dto.request.OrderRequest;
import com.retail.inventory.order_service.api.dto.request.ReserveRequest;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.domain.exception.ReservationFailedException;
import com.retail.inventory.order_service.domain.exception.ValidationException;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderItem;
import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.OrderRepository;
import com.retail.inventory.order_service.domain.repository.RetryRepository;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final RetryRepository retryRepo;
    private final ProductService proService;
    private final InventoryClient client;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepo, RetryRepository retryRepo, ProductService proService, InventoryClient client, OrderEventProducer eventProducer) {
        this.orderRepo = orderRepo;
        this.retryRepo = retryRepo;
        this.proService = proService;
        this.client = client;
        this.eventProducer = eventProducer;
    }

    private void validate(OrderRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new ValidationException("Order must contain at least 1 item");
        }

        for (OrderItemRequest item : req.items()) {
            if (item.sku() == null || item.sku().isBlank()) {
                throw new ValidationException("Items must have a SKU");
            }

            if (item.quantity() <= 0) {
                throw new ValidationException("Quantity must be greater than 0");
            }
        }
    }

    @Transactional
    public Order createOrder(OrderRequest req) {
        // ensure order is a valid request
        validate(req);

        // validate and enrich products from ProductService
        List<OrderItem> items = req.items()
                .stream()
                .map(dto -> {
                    ProductSnapshotEntity snapshot = proService.getProductBySku(dto.sku());
                    return new OrderItem(
                            snapshot.getSku(),
                            dto.quantity(),
                            snapshot.getPrice()
                    );
                })
                .toList();

        try {
            // attempt to reserve stock in inventory_service
            ReserveResponse reserved = reserve(new ReserveRequest(items));

            if (reserved.success()) {
                // set up successful order to communicate to service and publish to repo
                Order order = new Order(
                        OrderStatus.CONFIRMED,
                        items
                );

                Order saved = orderRepo.save(order);
                items.forEach(i -> i.setOrder(saved));

                // publish event to kafka
                OrderCreatedEvent event = OrderCreatedEvent.fromEntity(saved);
                eventProducer.publish(event);

                return saved;
            } else {
                throw new ReservationFailedException("Reservation failed");
            }
        } catch (Exception e) {
            return new Order(
                    OrderStatus.PENDING,
                    items
            );
        }
    }

    @Retry(name = "inventoryRetry", fallbackMethod = "reserveFallback")
    @CircuitBreaker(name = "inventoryCircuitBreaker", fallbackMethod = "reserveFallback")
    public ReserveResponse reserve(ReserveRequest req) {
        ReserveResponse res = client.reserve(req);

        if (!res.success()) {
            throw new ReservationFailedException("Reservation rejected");
        }

        return res;
    }

    public ReserveResponse reserveFallback(ReserveRequest items, Throwable t) {
        ReserveResponse res;

        retryRepo.save(new OrderRetry(new Order(OrderStatus.PENDING, items.items()), LocalDateTime.now()));
        res = new ReserveResponse(false, "Queued for retry");

        return res;
    }
}
