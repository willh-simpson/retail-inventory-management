package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.request.ReserveRequest;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.repository.OrderRepository;
import com.retail.inventory.order_service.domain.repository.RetryRepository;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetryService {
    private final RetryRepository retryRepo;
    private final OrderRepository orderRepo;
    private final InventoryClient client;
    private final OrderEventProducer eventProducer;

    public RetryService(RetryRepository retryRepo, OrderRepository orderRepo, InventoryClient client, OrderEventProducer eventProducer) {
        this.retryRepo = retryRepo;
        this.orderRepo = orderRepo;
        this.client = client;
        this.eventProducer = eventProducer;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${retries.process.interval:30000}")
    public void processRetries() {
        List<OrderRetry> retries = retryRepo.findAll();

        for (OrderRetry retry : retries) {
            try {
                orderRepo.findById(retry.getOrder().getId()).ifPresent(order -> handleRetry(retry, order));
            } catch (Exception e) {
                retry.incrementCount();
                retryRepo.save(retry);
            }
        }
    }

    private void handleRetry(OrderRetry retry, Order order) {
        ReserveResponse res = client.reserve(new ReserveRequest(order.getItems()));

        if (res.success()) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepo.save(order);

            eventProducer.publish(OrderCreatedEvent.fromEntity(order));
            retryRepo.delete(retry);
        } else {
            retry.incrementCount();
            retryRepo.save(retry);
        }
    }
}
