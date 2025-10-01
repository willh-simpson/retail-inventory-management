package com.retail.inventory.inventory_service.infrastructure.messaging;

import com.retail.inventory.inventory_service.api.dto.snapshot.CategorySnapshot;
import com.retail.inventory.inventory_service.domain.model.EventVersion;
import com.retail.inventory.inventory_service.domain.repository.EventVersionRepository;
import com.retail.inventory.common.messaging.Envelope;
import com.retail.inventory.common.messaging.model.EventType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CategoryEventProducer {
    private final KafkaTemplate<String, Object> kafka;
    private final EventVersionRepository evRepo;

    @Value("${kafka.topics.categorySnapshots}")
    private String topic;

    public CategoryEventProducer(KafkaTemplate<String, Object> kafka, EventVersionRepository evRepo) {
        this.kafka = kafka;
        this.evRepo = evRepo;
    }

    @Transactional
    public void publish(CategorySnapshot snapshot) {
        String aggregateKey = snapshot.name();

        // get current version or initialize version
        EventVersion ev = evRepo
                .findById(aggregateKey)
                .orElse(new EventVersion(aggregateKey, 0L));

        long nextVersion = ev.getVersion() + 1;
        ev.setVersion(nextVersion);
        evRepo.save(ev);

        // wrap payload in envelope
        Envelope<CategorySnapshot> envelope = new Envelope<>(
                UUID.randomUUID().toString(),
                EventType.CATEGORY_UPDATED,
                aggregateKey,
                nextVersion,
                LocalDateTime.now(),
                snapshot
        );

        kafka.send(topic, aggregateKey, envelope);
    }
}
