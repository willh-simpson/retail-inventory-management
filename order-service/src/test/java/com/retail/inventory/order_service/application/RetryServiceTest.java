package com.retail.inventory.order_service.application;

import com.retail.inventory.common.messaging.InventoryReservationStatus;
import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.application.service.RetryService;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderItem;
import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.repository.order.OrderRepository;
import com.retail.inventory.order_service.domain.repository.order.RetryRepository;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RetryServiceTest {
    @Mock
    private RetryRepository retryRepo;
    @Mock
    private OrderRepository orderRepo;
    @Mock
    private InventoryClient client;
    @Mock
    private OrderEventProducer eventProducer;

    @InjectMocks
    private RetryService retryService;

    @Test
    void processRetry_success() {
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                Arrays.asList(
                        new OrderItem("00589837", 5, 9.99),
                        new OrderItem("00010001", 4, 19.99)
                )
        );
        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());

        when(retryRepo.findAll()).thenReturn(List.of(retry));
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(client.reserve(any())).thenReturn(new ReserveResponse(true, InventoryReservationStatus.SUCCESS, "Order reserved"));

        retryService.processRetries();

        verify(orderRepo).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(eventProducer).publish(any(OrderCreatedEvent.class));
        verify(retryRepo).delete(retry);
    }

    @Test
    void processRetry_failure() {
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                Arrays.asList(
                        new OrderItem("00589837", 5, 9.99),
                        new OrderItem("00010001", 4, 19.99)
                )
        );
        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());

        when(retryRepo.findAll()).thenReturn(List.of(retry));
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(client.reserve(any())).thenReturn(new ReserveResponse(false, InventoryReservationStatus.ERROR, "Could not reserve order"));

        retryService.processRetries();

        verify(retryRepo).save(argThat(r -> r.getRetryCount() == 1));
        verify(eventProducer, never()).publish(any());
    }
}
