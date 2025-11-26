package com.retail.inventory.order_service.application.service;

import com.retail.inventory.common.messaging.InventoryReservationStatus;
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
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.repository.order.OrderRepository;
import com.retail.inventory.order_service.domain.repository.order.RetryRepository;
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
            throw new ValidationException(ValidationException.Message.NO_ITEMS);
        }

        for (OrderItemRequest item : req.items()) {
            if (item.sku() == null || item.sku().isBlank()) {
                throw new ValidationException(ValidationException.Message.MISSING_SKU);
            }

            if (item.quantity() <= 0) {
                throw new ValidationException(
                        ValidationException.Message.INVALID_QUANTITY,
                        " -> " + item.sku() + ": " + item.quantity()
                );
            }
        }
    }

    private double getTotal(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
    }

    public Order getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order createOrder(OrderRequest req) {
        // ensure order is a valid request
        validate(req);

        // validate and enrich products from ProductService
        List<OrderItem> items = req.items()
                .stream()
                .map(dto -> {
                    ProductSnapshot snapshot = proService.getProductBySku(dto.sku());
                    return new OrderItem(
                            snapshot.getSku(),
                            dto.quantity(),
                            snapshot.getPrice()
                    );
                })
                .toList();

        ReserveResponse result;
        try {
            // attempt to reserve stock in inventory_service
            result = reserve(new ReserveRequest(items));
        } catch (Exception e) {
            result = ReserveResponse.systemError("Unexpected error during reservation");
        }

        return switch (result.status()) {
            case SUCCESS -> {
                Order order = new Order(
                        OrderStatus.CONFIRMED,
                        items
                );

                items.forEach(i -> i.setOrder(order));
                order.setTotal(getTotal(items));

                Order saved = orderRepo.save(order);

                // publish event to kafka
                eventProducer.publish(OrderCreatedEvent.fromEntity(saved));

                yield saved;
            }

            case INSUFFICIENT_STOCK, OUT_OF_STOCK, DISCONTINUED -> throw new ReservationFailedException(result.message());

            case ERROR -> {
                // queue order for retry
                Order order = new Order(
                        OrderStatus.PENDING_RETRY,
                        items
                );
                items.forEach(i -> i.setOrder(order));
                order.setTotal(getTotal(items));

                // retry order needs to be persisted so retry system can find it
                yield orderRepo.save(order);
            }
        };
    }

    @Retry(name = "inventoryRetry", fallbackMethod = "reserveFallback")
    @CircuitBreaker(name = "inventoryCircuitBreaker", fallbackMethod = "reserveFallback")
    public ReserveResponse reserve(ReserveRequest req) {
        ReserveResponse res = client.reserve(req);
        InventoryReservationStatus status = res.status();

        return switch (status) {
            // business failure: do not retry
            case INSUFFICIENT_STOCK , OUT_OF_STOCK, DISCONTINUED -> res;

            // technical failure: allow retry
            case ERROR -> throw new ReservationFailedException("Error during reservation");

            case SUCCESS -> res;
        };
    }

    public ReserveResponse reserveFallback(ReserveRequest req, Throwable t) {
        ReserveResponse res;

        retryRepo.save(
                new OrderRetry(
                        new Order(
                                OrderStatus.PENDING_RETRY,
                                req.items()),
                        LocalDateTime.now()
                )
        );
        res = ReserveResponse.systemError("Queued for retry");

        return res;
    }
}
