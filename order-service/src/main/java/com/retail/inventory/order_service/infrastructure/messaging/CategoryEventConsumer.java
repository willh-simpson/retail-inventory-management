package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.CategorySnapshot;
import com.retail.inventory.order_service.domain.model.CategorySnapshotEntity;
import com.retail.inventory.order_service.domain.repository.CategorySnapshotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CategoryEventConsumer {
    private final CategorySnapshotRepository snapshotRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper;

    public CategoryEventConsumer(CategorySnapshotRepository snapshotRepo, MeterRegistry meterRegistry, ObjectMapper mapper) {
        this.snapshotRepo = snapshotRepo;
        this.meterRegistry = meterRegistry;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topics.categorySnapshots}", groupId = "${kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope> record) {
        try {
            Envelope envelope = record.value();
            CategorySnapshot payload = mapper.convertValue(envelope.getPayload(), CategorySnapshot.class);

            // check version
            CategorySnapshotEntity existing = snapshotRepo
                    .findByName(payload.name())
                    .orElse(null);

            if (existing == null || envelope.getVersion() > existing.getVersion()) {
                snapshotRepo.save(CategorySnapshot.toEntity(payload, envelope.getVersion()));
                meterRegistry.counter("categories.events.consumed").increment();
            } else {
                meterRegistry.counter("categories.events.ignored_stale").increment();
            }
        } catch (Exception e) {
            meterRegistry.counter("categories.events.failed").increment();

            throw e;
        }
    }
}
