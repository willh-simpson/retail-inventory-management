package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.api.dto.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.request.OrderRequest;
import com.retail.inventory.order_service.api.dto.request.ReserveRequest;
import com.retail.inventory.order_service.domain.model.Order;
import com.retail.inventory.order_service.domain.model.OrderItem;
import com.retail.inventory.order_service.domain.model.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.OrderRepository;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final ProductService proService;
    private final InventoryClient client;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepo, ProductService proService, InventoryClient client, OrderEventProducer eventProducer) {
        this.orderRepo = orderRepo;
        this.proService = proService;
        this.client = client;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Order createOrder(OrderRequest req) {
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

        // reserve stock in inventory_service
        for (OrderItem item : items) {
            boolean reserved = client.reserve(new ReserveRequest(item.getSku(), item.getQuantity()));

            if (!reserved) {
                throw new RuntimeException("Insufficient stock for " + item.getSku());
            }
        }

        // create and persist order
        Order order = new Order(
                "CREATED",
                items
        );
        items.forEach(i -> i.setOrder(order));
        Order saved = orderRepo.save(order);

        // publish event to kafka
        OrderCreatedEvent event = OrderCreatedEvent.fromEntity(saved);
        eventProducer.publish(event);

        return saved;
    }
}
