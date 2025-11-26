package com.retail.inventory.order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.InventoryReservationStatus;
import com.retail.inventory.order_service.OrderServiceApplication;
import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.api.dto.response.OrderResponse;
import com.retail.inventory.order_service.api.dto.response.ReserveResponse;
import com.retail.inventory.order_service.application.service.ProductService;
import com.retail.inventory.order_service.config.TestMetricsConfig;
import com.retail.inventory.order_service.config.TestMongoConfig;
import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderItem;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.repository.order.OrderRepository;
import com.retail.inventory.order_service.api.dto.request.OrderItemRequest;
import com.retail.inventory.order_service.api.dto.request.OrderRequest;
import com.retail.inventory.order_service.infrastructure.client.InventoryClient;
import com.retail.inventory.order_service.infrastructure.messaging.CategoryEventConsumer;
import com.retail.inventory.order_service.infrastructure.messaging.InventoryEventConsumer;
import com.retail.inventory.order_service.infrastructure.messaging.OrderEventProducer;
import com.retail.inventory.order_service.infrastructure.messaging.ProductEventConsumer;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {OrderServiceApplication.class},
        properties = {
                "spring.autoconfigure.exclude=" +
                        "io.github.resilience4j.springboot3.autoconfigure.Resilience4jMicrometerAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration",
                "resilience4j.metrics.enabled=false",
                "management.metrics.export.simple.enabled=false"
        }
)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMetricsConfig.class, TestMongoConfig.class})
public class OrderIntegrationTest {
    @MockBean
    private CategoryEventConsumer categoryEventConsumer;
    @MockBean
    private InventoryEventConsumer inventoryEventConsumer;
    @MockBean
    private ProductEventConsumer productEventConsumer;
    @MockBean
    private ProductService productService;
    @MockBean
    private InventoryClient inventoryClient;
    @MockBean
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    @SpyBean
    private OrderEventProducer eventProducer;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private OrderRepository orderRepo;

    @BeforeEach
    void resetSequences() {
        jdbc.execute("TRUNCATE TABLE orders RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE order_items RESTART IDENTITY CASCADE");
    }

    @BeforeEach
    void setupMocks() {
        when(productService.getProductBySku(anyString()))
                .thenReturn(new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test"),
                        1L
                ));

        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        true,
                        InventoryReservationStatus.SUCCESS,
                        "Reserved successfully"
                ));

        doNothing().when(eventProducer).publish(any());

        orderRepo.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrderAndRetrieve_success() throws Exception {
        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));

        // create order and persist to db
        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].sku").value("00589837"));

        // verify order persisted
        List<Order> orders = orderRepo.findAll();
        assertThat(orders).hasSize(1);
        Order saved = orders.get(0);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // retrieve order
        mvc.perform(get("/api/orders/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.items[0].sku").value("00589837"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_failure_businessFailure() throws Exception {
        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));

        /*
         * insufficient stock
         */
        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        false,
                        InventoryReservationStatus.INSUFFICIENT_STOCK,
                        "Insufficient stock"
                ));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock"));

        Assertions.assertEquals(0, orderRepo.count());

        /*
         * out of stock
         */

        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        false,
                        InventoryReservationStatus.OUT_OF_STOCK,
                        "Out of stock"
                ));

        mvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.message").value("Out of stock"));

        Assertions.assertEquals(0, orderRepo.count());

        /*
         * discontinued
         */
        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        false,
                        InventoryReservationStatus.DISCONTINUED,
                        "Item discontinued"
                ));

        mvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"))
                .andExpect(jsonPath("$.message").value("Item discontinued"));

        Assertions.assertEquals(0, orderRepo.count());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_failure_enqueuesRetry() throws Exception {
        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));

        when (inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        false,
                        InventoryReservationStatus.ERROR,
                        "Unexpected error during reservation"
                ));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_RETRY"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_failure_correctItemsPersisted() throws Exception {
        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));

        when(inventoryClient.reserve(any()))
                .thenThrow(new RuntimeException("Inventory service offline"));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_RETRY"));

        // verify order persisted
        List<Order> orders = orderRepo.findAll();
        Assertions.assertEquals(1, orders.size());

        Order saved = orders.get(0);
        Assertions.assertEquals(OrderStatus.PENDING_RETRY, saved.getStatus());

        // validate items
        List<OrderItem> items = saved.getItems();
        Assertions.assertEquals(1, items.size());

        OrderItem savedItem = items.get(0);
        Assertions.assertEquals("00589837", savedItem.getSku());
        Assertions.assertEquals(2, savedItem.getQuantity());
        Assertions.assertEquals(9.99, savedItem.getPrice());

        // item.order_id should match order.id
        Assertions.assertNotNull(saved.getId());
        Assertions.assertEquals(saved.getId(), savedItem.getOrder().getId());

        // validate total: 9.99 * 2 = 19.98
        Assertions.assertEquals(19.98, saved.getTotal(), 0.001);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_failure_validation() throws Exception {
        /*
         * negative quantity
         */
        OrderItemRequest item = new OrderItemRequest("00589837", -1);
        OrderRequest req = new OrderRequest(List.of(item));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation failed"))
                .andExpect(jsonPath("$.message").value("Quantity must be greater than 0 -> " + item.sku() + ": " + item.quantity()));

        Assertions.assertEquals(0, orderRepo.count());

        /*
         * missing sku
         */
        item = new OrderItemRequest("", 2);
        req = new OrderRequest(List.of(item));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation failed"))
                .andExpect(jsonPath("$.message").value("Items must have SKU"));

        Assertions.assertEquals(0, orderRepo.count());

        /*
         * empty items list
         */
        req = new OrderRequest(List.of());

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation failed"))
                .andExpect(jsonPath("$.message").value("Order must contain at least 1 item"));

        Assertions.assertEquals(0, orderRepo.count());

        /*
         * malformed request body
         */
        String malformedReq = """
                { "malformed_field": "malformed_value" }
                """;

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedReq))
                .andExpect(status().isBadRequest());

        Assertions.assertEquals(0, orderRepo.count());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetOrderById_success() throws Exception {
        OrderItem item1 = new OrderItem(
                "00589837",
                2,
                9.99
        );
        OrderItem item2 = new OrderItem(
                "00010001",
                3,
                19.99
        );
        Order order = new Order(
                OrderStatus.CONFIRMED,
                List.of(item1, item2)
        );

        item1.setOrder(order);
        item2.setOrder(order);
        order.setTotal((item1.getQuantity() * item1.getPrice()) + (item2.getQuantity() * item2.getPrice()));

        Order saved = orderRepo.save(order);

        mvc.perform(get("/api/orders/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.total").value((item1.getQuantity() * item1.getPrice()) + (item2.getQuantity() * item2.getPrice())))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].sku").value("00589837"))
                .andExpect(jsonPath("$.items[1].sku").value("00010001"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_success_emitsKafkaEvent() throws Exception {
        // remove setupMocks() mocking so publish() works normally
        Mockito.reset(eventProducer);

        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));
        ProductSnapshot product = new ProductSnapshot(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );

        when(productService.getProductBySku("00589837")).thenReturn(product);
        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        true,
                        InventoryReservationStatus.SUCCESS,
                        "Reserved successfully"
                ));

        MvcResult result = mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        Long resultId = mapper.readTree(json).get("id").asLong();

        // assert db validation
        Order saved = orderRepo.findById(resultId).orElseThrow();
        Assertions.assertEquals(OrderStatus.CONFIRMED, saved.getStatus());
        Assertions.assertEquals(1, saved.getItems().size());

        // capture kafka event
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventProducer, times(1)).publish(eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();

        // assert event fields match saved order
        Assertions.assertEquals(saved.getId(), event.orderId());
        Assertions.assertEquals(saved.getTotal(), event.total());
        Assertions.assertEquals(saved.getItems().size(), event.items().size());

        // validate event item content
        Assertions.assertEquals("00589837", event.items().get(0).getSku());
        Assertions.assertEquals(2, event.items().get(0).getQuantity());
        Assertions.assertEquals(9.99, event.items().get(0).getPrice());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_success_multipleItems() throws Exception {
        OrderItemRequest item1 = new OrderItemRequest("00589837", 2);
        OrderItemRequest item2 = new OrderItemRequest("00010001", 3);
        OrderRequest req = new OrderRequest(List.of(item1, item2));

        when(productService.getProductBySku("00589837"))
                .thenReturn(new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test"),
                        1L
                ));

        when(productService.getProductBySku("00010001"))
                .thenReturn(new ProductSnapshot(
                        "00010001",
                        "test 2",
                        "desc 2",
                        19.99,
                        Map.of("category", "test"),
                        1L
                ));

        mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].sku").value("00589837"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].sku").value("00010001"))
                .andExpect(jsonPath("$.items[1].quantity").value(3));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRetryOrder_canBeRetrievedLater() {
        OrderItem item1 = new OrderItem("00589837", 2, 9.99);
        OrderItem item2 = new OrderItem("00010001", 3, 19.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                List.of(item1, item2)
        );

        item1.setOrder(order);
        item2.setOrder(order);
        order.setTotal(
                (item1.getQuantity() * item1.getPrice()) +
                        (item2.getQuantity() * item2.getPrice())
        );

        Order saved = orderRepo.save(order);
        List<Order> pendingOrders = orderRepo.findByStatus(OrderStatus.PENDING_RETRY);

        // validate retry orders are accurately persisted for retry
        Assertions.assertEquals(1, pendingOrders.size());

        Order found = pendingOrders.get(0);
        Assertions.assertEquals(saved.getId(), found.getId());
        Assertions.assertEquals(OrderStatus.PENDING_RETRY, found.getStatus());

        Assertions.assertEquals(2, found.getItems().size());
        Assertions.assertEquals("00589837", found.getItems().get(0).getSku());
        Assertions.assertEquals("00010001", found.getItems().get(1).getSku());
        Assertions.assertEquals(saved.getTotal(), found.getTotal(), .001);

        Assertions.assertNotNull(found.getItems().get(0).getOrder());
        Assertions.assertEquals(found, found.getItems().get(0).getOrder());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_totalIsAccurate() throws Exception {
        OrderItemRequest item1 = new OrderItemRequest("00589837", 2);
        OrderItemRequest item2 = new OrderItemRequest("00010001", 3);
        OrderRequest req = new OrderRequest(List.of(item1, item2));

        ProductSnapshot product1 = new ProductSnapshot(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );
        ProductSnapshot product2 = new ProductSnapshot(
                "00010001",
                "test 2",
                "desc 2",
                19.99,
                Map.of("category", "test"),
                1L
        );

        double expectedTotal =
                (product1.getPrice() * item1.quantity()) +
                (product2.getPrice() * item2.quantity());

        when(productService.getProductBySku("00589837")).thenReturn(product1);
        when(productService.getProductBySku("00010001")).thenReturn(product2);
        when(inventoryClient.reserve(any()))
                .thenReturn(new ReserveResponse(
                        true,
                        InventoryReservationStatus.SUCCESS,
                        "Reserved successfully"
                ));

        MvcResult result = mvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        OrderResponse response = mapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );

        // fetch saved order
        Order saved = orderRepo.findById(response.id()).orElseThrow();
        // assert total is equal within precision tolerance
        Assertions.assertEquals(expectedTotal, saved.getTotal(), .000001);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateOrder_timeoutFailure_pendingRetry() throws Exception {
        OrderItemRequest item = new OrderItemRequest("00589837", 2);
        OrderRequest req = new OrderRequest(List.of(item));

        ProductSnapshot product = new ProductSnapshot(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );

        when(productService.getProductBySku("00589837")).thenReturn(product);
        when(inventoryClient.reserve(any()))
                .thenThrow(new RuntimeException("Inventory timeout"));

        MvcResult result = mvc.perform(post("/api/orders")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();
        OrderResponse response = mapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );
        Order saved = orderRepo.findById(response.id()).orElseThrow();

        // order is pending retry
        Assertions.assertEquals(OrderStatus.PENDING_RETRY, saved.getStatus());

        // no kafka event emitted
        verify(eventProducer, never()).publish(any());
    }
}
