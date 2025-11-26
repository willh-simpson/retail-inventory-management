package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.request.ReserveRequest;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.domain.exception.RetryException;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.repository.order.OrderRepository;
import com.retail.inventory.order_service.domain.repository.order.RetryRepository;
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
            Long orderId = retry.getOrder().getId();

            try {
                orderRepo
                        .findById(orderId)
                        .ifPresentOrElse(
                                order -> handleRetry(retry, order),
                                () -> {
                                    throw new RetryException(RetryException.Message.ORIGIN_ORDER_NOT_FOUND);
                                }
                        );
            } catch (RetryException ex) {
                if (ex.getMessage().contains(RetryException.Message.ORIGIN_ORDER_NOT_FOUND.toString())) {
                    System.err.println(ex.getMessage() + " -> " + orderId);
                    System.err.println("Deleting corrupted retry ID -> " + retry.getId());

                    retryRepo.delete(retry);
                }
            } catch (Exception ex) {
                System.err.println("Unexpected error when fetching order");

                retry.incrementCount();
                retryRepo.save(retry);
            }
        }
    }

    private void handleRetry(OrderRetry retry, Order order) {
        // don't process retries on an already confirmed order
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            retryRepo.delete(retry);

            return;
        }

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
