package com.retail.inventory.order_service.application;

import com.retail.inventory.common.messaging.InventoryReservationStatus;
import com.retail.inventory.order_service.OrderServiceApplication;
import com.retail.inventory.order_service.api.dto.request.OrderRequest;
import com.retail.inventory.order_service.api.dto.request.OrderItemRequest;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.application.service.OrderService;
import com.retail.inventory.order_service.application.service.ProductService;
import com.retail.inventory.order_service.config.NoRetryConfig;
import com.retail.inventory.order_service.domain.exception.ValidationException;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.repository.order.OrderRepository;
import com.retail.inventory.order_service.domain.repository.order.RetryRepository;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Map;

/**
 * normally this unit test should not be a SpringBootTest,
 * but unsuccessful order reservations results in
 * orderService.createOrder() returning null due to reserveFallback()
 * not being called by Mockito.
 */
@SpringBootTest(classes = {OrderServiceApplication.class})
@Import(NoRetryConfig.class)
public class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepo;
    @MockBean
    private RetryRepository retryRepo;
    @MockBean
    private OrderEventProducer eventProducer;
    @MockBean
    private ProductService proService;
    @MockBean
    private InventoryClient client;

    private OrderService orderServiceSpy;

    @Test
    void testCreateOrder_success_persistAndPublish() {
        OrderRequest req = new OrderRequest(
                Arrays.asList(
                        new OrderItemRequest("00589837", 5),
                        new OrderItemRequest("00010001", 4)
                )
        );

        // mock reserving an item
        when(client.reserve(any())).thenReturn(
                new ReserveResponse(true, InventoryReservationStatus.SUCCESS, "Item reserved")
        );
        // mock finding item
        when(proService.getProductBySku(req.items().get(0).sku())).thenReturn(new ProductSnapshot("00589837", "test", "desc", 9.99, Map.of("category", "test"), 1L));
        when(proService.getProductBySku(req.items().get(1).sku())).thenReturn(new ProductSnapshot("00010001", "test 2", "desc", 19.99, Map.of("category", "test"), 1L));
        // make repo save() return the order passed in
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.createOrder(req);

        // verify order is created with multiple items and persisted
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        // verify methods were called expected amount of times
        verify(orderRepo, times(1)).save(any(Order.class));
        verify(proService, times(2)).getProductBySku(any());
        verify(client, times(1)).reserve(any());
        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void testCreateOrder_failure_enqueueRetry() {
        OrderRequest req = new OrderRequest(
                Arrays.asList(
                        new OrderItemRequest("00589837", 5),
                        new OrderItemRequest("00010001", 4)
                )
        );

        when(proService.getProductBySku(req.items().get(0).sku())).thenReturn(new ProductSnapshot("00589837", "test", "desc", 9.99, Map.of("category", "test"), 1L));
        when(proService.getProductBySku(req.items().get(1).sku())).thenReturn(new ProductSnapshot("00010001", "test 2", "desc", 19.99, Map.of("category", "test"), 1L));

        // mock not being able to reserve item
        when(client.reserve(any()))
                .thenReturn(ReserveResponse.systemError("Could not reserve order"));
        when(orderRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.createOrder(req);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_RETRY);
        verify(eventProducer, never()).publish(any());
    }

    @Test
    void testCreateOrder_throws_enqueueRetry() {
        OrderRequest req = new OrderRequest(
                Arrays.asList(
                        new OrderItemRequest("00589837", 5),
                        new OrderItemRequest("00010001", 4)
                )
        );

        when(proService.getProductBySku(req.items().get(0).sku())).thenReturn(new ProductSnapshot("00589837", "test", "desc", 9.99, Map.of("category", "test"), 1L));
        when(proService.getProductBySku(req.items().get(1).sku())).thenReturn(new ProductSnapshot("00010001", "test 2", "desc", 19.99, Map.of("category", "test"), 1L));
        when(client.reserve(any()))
                .thenThrow(new RuntimeException("Could not reserve order"));
        when(orderRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.createOrder(req);

        // verify exception was handled
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_RETRY);
        verify(eventProducer, never()).publish(any());
    }

    @Test
    void testCreateOrder_throws_invalidQuantity() {
        OrderRequest req = new OrderRequest(
                Arrays.asList(
                        new OrderItemRequest("00589837", 0),
                        new OrderItemRequest("00010001", -1)
                )
        );

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Quantity must be greater than 0");

        verifyNoInteractions(client, eventProducer, retryRepo);
    }

    @Test
    void testCreateOrder_throws_emptyRequest() {
        OrderRequest req = new OrderRequest(
                null
        );

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Order must contain at least 1 item");

        verifyNoInteractions(client, eventProducer, retryRepo);
    }

    @Test
    void testCreateOrder_throws_noSku() {
        OrderRequest req = new OrderRequest(
                Arrays.asList(
                        new OrderItemRequest("", 0),
                        new OrderItemRequest(null, -1)
                )
        );

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ValidationException.Message.MISSING_SKU.toString());

        verifyNoInteractions(client, eventProducer, retryRepo);
    }
}
