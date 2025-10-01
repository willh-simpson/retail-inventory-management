package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.snapshot.InventorySnapshot;
import com.retail.inventory.order_service.domain.model.InventorySnapshotEntity;
import com.retail.inventory.order_service.domain.repository.InventorySnapshotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventConsumer {
    private final InventorySnapshotRepository snapshotRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper;

    public InventoryEventConsumer(InventorySnapshotRepository snapshotRepo, MeterRegistry meterRegistry, ObjectMapper mapper) {
        this.snapshotRepo = snapshotRepo;
        this.meterRegistry = meterRegistry;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.inventorySnapshots}", groupId = "${kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope> record) {
        try {
            Envelope envelope = record.value();
            InventorySnapshot payload = mapper.convertValue(envelope.getPayload(), InventorySnapshot.class);

            // check version
            InventorySnapshotEntity existing = snapshotRepo
                    .findByProductSku(payload.productSku())
                    .orElse(null);

            if (existing == null || envelope.getVersion() > existing.getVersion()) {
                snapshotRepo.save(InventorySnapshot.toEntity(payload, envelope.getVersion()));
                meterRegistry.counter("inventory.events.consumed").increment();
            } else {
                meterRegistry.counter("inventory.events.ignored_stale").increment();
            }
        } catch (Exception e) {
            meterRegistry.counter("inventory.events.failed").increment();

            throw e;
        }
    }
}
