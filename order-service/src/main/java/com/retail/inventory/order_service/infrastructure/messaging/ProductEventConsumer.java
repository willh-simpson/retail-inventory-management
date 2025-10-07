package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.ProcessedEventRepository;
import com.retail.inventory.order_service.domain.repository.ProductSnapshotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {
    private final ProductSnapshotRepository snapshotRepo;
    private final ProcessedEventRepository eventRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper;

    public ProductEventConsumer(ProductSnapshotRepository snapshotRepo, ProcessedEventRepository eventRepo, MeterRegistry meterRegistry, ObjectMapper mapper) {
        this.snapshotRepo = snapshotRepo;
        this.eventRepo = eventRepo;
        this.meterRegistry = meterRegistry;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.productSnapshots}", groupId = "${kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope<ProductSnapshot>> record) {
        try {
            Envelope<ProductSnapshot> envelope = record.value();
            ProductSnapshot payload = mapper.convertValue(envelope.getPayload(), ProductSnapshot.class);

            // check if event is already processed
            if (eventRepo.existsById(envelope.getEventId())) {
                meterRegistry.counter("products.events.duplicate").increment();
                return;
            }

            // check version
            ProductSnapshotEntity existing = snapshotRepo
                    .findBySku(payload.sku())
                    .orElse(null);

            if (existing == null || envelope.getVersion() > existing.getVersion()) {
                snapshotRepo.save(ProductSnapshot.toEntity(payload, envelope.getVersion()));
                meterRegistry.counter("products.events.consumed").increment();
            } else {
                meterRegistry.counter("products.events.ignored_stale").increment();
            }
        } catch (Exception e) {
            meterRegistry.counter("products.events.failed").increment();
        }
    }
}
