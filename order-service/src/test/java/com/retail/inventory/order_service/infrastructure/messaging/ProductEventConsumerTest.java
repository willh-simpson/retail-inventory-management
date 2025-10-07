package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.common.messaging.model.EventType;
import com.retail.inventory.order_service.api.dto.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.ProcessedEventRepository;
import com.retail.inventory.order_service.domain.repository.ProductSnapshotRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductEventConsumerTest {
    @Mock
    private ProductSnapshotRepository snapshotRepo;
    @Mock
    private ProcessedEventRepository eventRepo;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    ProductEventConsumer eventConsumer;

    @Test
    void testConsume_duplicateEvent() {
        String eventId = UUID.randomUUID().toString();
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                eventId,
                EventType.PRODUCT_UPDATED,
                "00589837",
                1L,
                LocalDateTime.now(),
                new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test")
                )
        );
        ConsumerRecord<String, Envelope<ProductSnapshot>> record = new ConsumerRecord<>(
                "product.snapshots",
                0,
                0L,
                envelope.getEventId(),
                envelope
        );

        // mock finding a duplicate event
        when(eventRepo.existsById(eventId)).thenReturn(true);
        // stub MeterRegistry used in consume()
        when(meterRegistry.counter(eq("products.events.duplicate"))).thenReturn(counter);
        when(mapper.convertValue(envelope.getPayload(), ProductSnapshot.class)).thenReturn(envelope.getPayload());

        eventConsumer.consume(record);

        verify(snapshotRepo, never()).save(any());
        verify(counter, times(1)).increment();
    }

    @Test
    void testConsume_staleEvent() {
        String eventId = UUID.randomUUID().toString();
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                eventId,
                EventType.PRODUCT_UPDATED,
                "00589837",
                1L,
                LocalDateTime.now(),
                new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test")
                )
        );
        ConsumerRecord<String, Envelope<ProductSnapshot>> record = new ConsumerRecord<>(
                "product.snapshots",
                0,
                0L,
                envelope.getEventId(),
                envelope
        );

        ProductSnapshotEntity existing = new ProductSnapshotEntity(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                2L
        );

        // event doesn't exist but item exists in snapshot repo
        when(eventRepo.existsById(eventId)).thenReturn(false);
        when(mapper.convertValue(envelope.getPayload(), ProductSnapshot.class)).thenReturn(envelope.getPayload());
        when(snapshotRepo.findBySku("00589837")).thenReturn(Optional.of(existing));
        when(meterRegistry.counter(eq("products.events.ignored_stale"))).thenReturn(counter);

        eventConsumer.consume(record);

        verify(snapshotRepo, never()).save(any());
        verify(counter, times(1)).increment();
    }

    @Test
    void testConsume_updateProduct() {
        String eventId = UUID.randomUUID().toString();
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                eventId,
                EventType.PRODUCT_UPDATED,
                "00589837",
                2L,
                LocalDateTime.now(),
                new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test")
                )
        );
        ConsumerRecord<String, Envelope<ProductSnapshot>> record = new ConsumerRecord<>(
                "product.snapshots",
                0,
                0L,
                envelope.getEventId(),
                envelope
        );

        ProductSnapshotEntity existing = new ProductSnapshotEntity(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );

        // event doesn't exist and item exists in repo but is an older version, so it should be updated
        when(eventRepo.existsById(eventId)).thenReturn(false);
        when(mapper.convertValue(envelope.getPayload(), ProductSnapshot.class)).thenReturn(envelope.getPayload());
        when(snapshotRepo.findBySku("00589837")).thenReturn(Optional.of(existing));
        when(meterRegistry.counter(eq("products.events.consumed"))).thenReturn(counter);

        eventConsumer.consume(record);

        // verify snapshot was updated
        verify(snapshotRepo, times(1)).save(any());
        verify(counter, times(1)).increment();
    }

    @Test
    void testConsume_throws() {
        String eventId = UUID.randomUUID().toString();
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                eventId,
                EventType.PRODUCT_UPDATED,
                "00589837",
                1L,
                LocalDateTime.now(),
                new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test")
                )
        );
        ConsumerRecord<String, Envelope<ProductSnapshot>> record = new ConsumerRecord<>(
                "product.snapshots",
                0,
                0L,
                envelope.getEventId(),
                envelope
        );

        when(mapper.convertValue(envelope.getPayload(), ProductSnapshot.class)).thenThrow(new RuntimeException("Could not consume"));
        when(meterRegistry.counter("products.events.failed")).thenReturn(counter);

        eventConsumer.consume(record);

        verify(snapshotRepo, never()).save(any());
        verify(counter, times(1)).increment();
    }

    @Test
    void testConsume_savesAndRecords() {
        String eventId = UUID.randomUUID().toString();
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                eventId,
                EventType.PRODUCT_UPDATED,
                "00589837",
                1L,
                LocalDateTime.now(),
                new ProductSnapshot(
                        "00589837",
                        "test",
                        "desc",
                        9.99,
                        Map.of("category", "test")
                )
        );
        ConsumerRecord<String, Envelope<ProductSnapshot>> record = new ConsumerRecord<>(
                "product.snapshots",
                0,
                0L,
                envelope.getEventId(),
                envelope
        );

        // mock event not existing in event repo
        when(eventRepo.existsById(eventId)).thenReturn(false);
        // stub MeterRegistry in consume()
        when(meterRegistry.counter(eq("products.events.consumed"))).thenReturn(counter);
        when(mapper.convertValue(envelope.getPayload(), ProductSnapshot.class)).thenReturn(envelope.getPayload());

        eventConsumer.consume(record);

        verify(snapshotRepo, times(1)).save(any());
        verify(counter, times(1)).increment();
    }
}
