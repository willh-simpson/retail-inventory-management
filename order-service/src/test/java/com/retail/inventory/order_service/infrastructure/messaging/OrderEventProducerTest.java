package com.retail.inventory.order_service.infrastructure.messaging;

import com.retail.inventory.order_service.api.dto.event.OrderCreatedEvent;
import com.retail.inventory.order_service.domain.model.order.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderEventProducerTest {
    @Mock
    private KafkaTemplate<String, Object> kafka;

    @InjectMocks
    private OrderEventProducer eventProducer;

    @Test
    void testPublish_callsKafka() {
        // assume order was created successfully
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                1L,
                Arrays.asList(
                        new OrderItem("00589837", 5, 9.99),
                        new OrderItem("00010001", 4, 19.99)
                ),
                (9.99 * 5.0) + (19.99 * 4.0),
                LocalDateTime.now()
        );

        // mock successful send
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafka.send(anyString(), anyString(), any())).thenReturn(future);

        eventProducer.publish(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("order.created"), anyString(), payloadCaptor.capture());

        Object sentPayload = payloadCaptor.getValue();
        assertThat(sentPayload).isInstanceOf(OrderCreatedEvent.class);
    }

    @Test
    void testPublish_kafkaFails() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                1L,
                Arrays.asList(
                        new OrderItem("00589837", 5, 9.99),
                        new OrderItem("00010001", 4, 19.99)
                ),
                (9.99 * 5.0) + (19.99 * 4.0),
                LocalDateTime.now()
        );
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka send failed"));

        assertThatCode(() -> eventProducer.publish(event)).doesNotThrowAnyException();

        verify(kafka).send("order.created", "1", event);
    }
}
