package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.snapshot.InventorySnapshotDto;
import com.retail.inventory.order_service.domain.model.event.ProcessedEvent;
import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.InventorySnapshotRepository;
import com.retail.inventory.order_service.domain.repository.order.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventConsumer {
    private final InventorySnapshotRepository snapshotRepo;
    private final ProcessedEventRepository processedRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper;

    public InventoryEventConsumer(InventorySnapshotRepository snapshotRepo, ProcessedEventRepository processedRepo, MeterRegistry meterRegistry, ObjectMapper mapper) {
        this.snapshotRepo = snapshotRepo;
        this.processedRepo = processedRepo;
        this.meterRegistry = meterRegistry;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${spring.kafka.topics.inventorySnapshots}", groupId = "${spring.kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope<InventorySnapshotDto>> record) {
        try {
            Envelope<InventorySnapshotDto> envelope = record.value();
            InventorySnapshotDto payload = mapper.convertValue(envelope.getPayload(), InventorySnapshotDto.class);

            // check if item has already been processed
            if (processedRepo.existsById(envelope.getEventId())) {
                return; // duplicate event: ignore
            }

            // check version
            InventorySnapshot existing = snapshotRepo
                    .findByProductSku(payload.productSku())
                    .orElse(null);

            if (existing == null || envelope.getVersion() > existing.getVersion()) {
                snapshotRepo.save(InventorySnapshotDto.toEntity(payload, envelope.getVersion()));
                meterRegistry.counter("inventory.events.consumed").increment();
            } else {
                meterRegistry.counter("inventory.events.ignored_stale").increment();
            }

            // mark event as processed
            processedRepo.save(new ProcessedEvent(envelope.getEventId()));
        } catch (Exception e) {
            meterRegistry.counter("inventory.events.failed").increment();
        }
    }
}
