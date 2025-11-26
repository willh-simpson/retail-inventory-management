package com.retail.inventory.order_service.integration;

import com.retail.inventory.common.messaging.InventoryReservationStatus;
import com.retail.inventory.order_service.OrderServiceApplication;
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
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = OrderServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@Transactional
public class OrderRetryIntegrationTest {
    /*
     * testcontainers
     */
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    /*
     * apply container connection info
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private RetryRepository retryRepo;
    @Autowired
    private RetryService retryService;
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @MockBean
    private InventoryClient client;

    private KafkaConsumer<String, OrderCreatedEvent> buildTestConsumer() {
        return new KafkaConsumer<>(Map.of(
                org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG,
                "testGroup",
                org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "true",
                org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class,
                org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES,
                "*",
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                OrderCreatedEvent.class.getName()
        ));
    }

    @Test
    void testRetryScheduler_success_confirmsAndDeletes() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());
        orderRepo.save(order);

        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());
        retryRepo.save(retry);

        when(client.reserve(any()))
                .thenReturn(new ReserveResponse(
                        true,
                        InventoryReservationStatus.SUCCESS,
                        "Reserved successfully"
                ));

        // manually trigger scheduler
        retryService.processRetries();

        // verify order is processed, removed from retry repo, kafka event emitted
        Order updatedOrder = orderRepo.findById(order.getId()).orElseThrow();

        Assertions.assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
        Assertions.assertTrue(retryRepo.findAll().isEmpty());

        // kafka assertion
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));
            ConsumerRecord<String, OrderCreatedEvent> record = KafkaTestUtils.getSingleRecord(consumer, "order.created");

            Assertions.assertEquals(order.getId().toString(), record.key());
            Assertions.assertEquals(order.getId(), record.value().orderId());
        }
    }

    @Test
    void testRetryScheduler_failure() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());
        orderRepo.save(order);

        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());
        retryRepo.save(retry);

        when(client.reserve(any()))
                .thenReturn(new ReserveResponse(
                        false,
                        InventoryReservationStatus.ERROR,
                        "Unexpected error during reservation"
                ));

        retryService.processRetries();
        Order updatedOrder = orderRepo.findById(order.getId()).orElseThrow();

        Assertions.assertEquals(OrderStatus.PENDING_RETRY, updatedOrder.getStatus());
        Assertions.assertEquals(1, retryRepo.findAll().size());
        Assertions.assertEquals(1, retryRepo.findAll().get(0).getRetryCount());

        // verify no kafka event was published
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll for events with short timeout
            var records = consumer.poll(java.time.Duration.ofSeconds(2));

            Assertions.assertEquals(0, records.count());
        }
    }

    @Test
    void testRetryScheduler_failure_handleException() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());
        orderRepo.save(order);

        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());
        retryRepo.save(retry);

        // exception being thrown while processing retry order should not mess up flow
        when(client.reserve(any())).thenThrow(new RuntimeException("Network error"));

        retryService.processRetries();
        Order updatedOrder = orderRepo.findById(order.getId()).orElseThrow();

        Assertions.assertEquals(OrderStatus.PENDING_RETRY, updatedOrder.getStatus());
        Assertions.assertEquals(1, retryRepo.findAll().size());
        Assertions.assertEquals(1, retryRepo.findAll().get(0).getRetryCount());

        // verify no kafka event was published
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll for events with short timeout
            var records = consumer.poll(java.time.Duration.ofSeconds(2));

            Assertions.assertEquals(0, records.count());
        }
    }

    @Test
    void testRetryScheduler_success_multipleOrders() {
        OrderItem item1 = new OrderItem("00589837", 2, 9.99);
        Order order1 = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item1))
        );
        OrderItem item2 = new OrderItem("00010001", 3, 19.99);
        Order order2 = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item2))
        );

        item1.setOrder(order1);
        item2.setOrder(order2);
        order1.setTotal(item1.getQuantity() * item1.getPrice());
        order2.setTotal(item2.getQuantity() * item2.getPrice());

        orderRepo.save(order1);
        orderRepo.save(order2);

        OrderRetry retry1 = new OrderRetry(order1, LocalDateTime.now());
        OrderRetry retry2 = new OrderRetry(order2, LocalDateTime.now());
        retryRepo.save(retry1);
        retryRepo.save(retry2);

        when(client.reserve(any()))
                .thenReturn(new ReserveResponse(
                        true,
                        InventoryReservationStatus.SUCCESS,
                        "Reserved successfully"
                ));

        retryService.processRetries();

        // verify each order is processed, removed from retry repo, kafka event emitted
        Order updatedOrder1 = orderRepo.findById(order1.getId()).orElseThrow();
        Order updatedOrder2 = orderRepo.findById(order2.getId()).orElseThrow();

        Assertions.assertEquals(OrderStatus.CONFIRMED, updatedOrder1.getStatus());
        Assertions.assertEquals(OrderStatus.CONFIRMED, updatedOrder2.getStatus());
        Assertions.assertTrue(retryRepo.findAll().isEmpty());

        // kafka assertion
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll up to 5 seconds for both records
            var records = consumer.poll(java.time.Duration.ofSeconds(5));
            Assertions.assertEquals(2, records.count());

            List<Long> receivedOrderIds = new ArrayList<>();
            records.records("order.created").forEach(r -> receivedOrderIds.add(r.value().orderId()));

            // verify an event was emitted for both orders
            Assertions.assertTrue(receivedOrderIds.contains(order1.getId()));
            Assertions.assertTrue(receivedOrderIds.contains(order2.getId()));
        }
    }

    @Test
    void testRetryScheduler_orderDoesNotExist() {
        Order fakeOrder = new Order();
        fakeOrder.setId(99999L); // non-existent primary key

        OrderRetry retry = new OrderRetry(fakeOrder, LocalDateTime.now());
        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> retryRepo.saveAndFlush(retry)

        );

        // verify no kafka event was published
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll for events with short timeout
            var records = consumer.poll(java.time.Duration.ofSeconds(2));

            Assertions.assertEquals(0, records.count());
        }
    }

    @Test
    void testRetryScheduler_orphanedRetry() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());

        orderRepo.save(order);

        OrderRetry retry = new OrderRetry(order, LocalDateTime.now());
        retryRepo.save(retry);

        // retry service should delete retry order if it's orphaned
        orderRepo.delete(order);
        retryService.processRetries();
        Assertions.assertEquals(0, retryRepo.findAll().size());

        // verify no kafka event was published
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll for events with short timeout
            var records = consumer.poll(java.time.Duration.ofSeconds(2));

            Assertions.assertEquals(0, records.count());
        }
    }

    @Test
    void testRetryScheduler_cannotDeletePendingOrder() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());

        orderRepo.save(order);
        retryRepo.saveAndFlush(new OrderRetry(order, LocalDateTime.now()));

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    orderRepo.delete(order);
                    orderRepo.flush();
                }
        );
    }

    @Test
    void testRetryScheduler_handleDesyncOrder() {
        OrderItem item = new OrderItem("00589837", 2, 9.99);
        Order order = new Order(
                OrderStatus.PENDING_RETRY,
                new ArrayList<>(List.of(item))
        );
        item.setOrder(order);
        order.setTotal(item.getQuantity() * item.getPrice());

        orderRepo.save(order);
        retryRepo.save(new OrderRetry(order, LocalDateTime.now()));

        // verify RetryService doesn't process already confirmed orders
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);
        retryService.processRetries();

        verify(client, never()).reserve(any());
        Assertions.assertEquals(0, retryRepo.findAll().size());

        // verify no kafka event was published
        try (Consumer<String, OrderCreatedEvent> consumer = buildTestConsumer()) {
            consumer.subscribe(List.of("order.created"));

            // poll for events with short timeout
            var records = consumer.poll(java.time.Duration.ofSeconds(2));

            Assertions.assertEquals(0, records.count());
        }
    }
}
