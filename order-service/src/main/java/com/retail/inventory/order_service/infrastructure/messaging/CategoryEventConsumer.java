package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.order_service.api.dto.snapshot.CategorySnapshotDto;
import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.CategorySnapshotRepository;
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

    @KafkaListener(topics = "${spring.kafka.topics.categorySnapshots}", groupId = "${spring.kafka.groupId}")
    public void consume(ConsumerRecord<String, Envelope<CategorySnapshotDto>> record) {
        try {
            Envelope<CategorySnapshotDto> envelope = record.value();
            CategorySnapshotDto payload = mapper.convertValue(envelope.getPayload(), CategorySnapshotDto.class);

            // check version
            CategorySnapshot existing = snapshotRepo
                    .findByName(payload.name())
                    .orElse(null);

            if (existing == null || envelope.getVersion() > existing.getVersion()) {
                snapshotRepo.save(CategorySnapshotDto.toEntity(payload, envelope.getVersion()));
                meterRegistry.counter("categories.events.consumed").increment();
            } else {
                meterRegistry.counter("categories.events.ignored_stale").increment();
            }
        } catch (Exception e) {
            meterRegistry.counter("categories.events.failed").increment();
        }
    }
}
