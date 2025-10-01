package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.model.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.ProductSnapshotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {
    private final ProductSnapshotRepository snapshotRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper;

    public ProductEventConsumer(ProductSnapshotRepository snapshotRepo, MeterRegistry meterRegistry, ObjectMapper mapper) {
        this.snapshotRepo = snapshotRepo;
        this.meterRegistry = meterRegistry;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.productSnapshots}", groupId = "${kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope> record) {
        try {
            Envelope envelope = record.value();
            ProductSnapshot payload = mapper.convertValue(envelope.getPayload(), ProductSnapshot.class);

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

            throw e;
        }
    }
}
