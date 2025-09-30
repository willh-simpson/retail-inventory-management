package com.retail.inventory.inventory_service.infrastructure.messaging;

import com.retail.inventory.inventory_service.api.dto.ProductSnapshot;
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
public class ProductEventProducer {
    private final KafkaTemplate<String, Object> kafka;
    private final EventVersionRepository evRepo;

    @Value("${kafka.topics.productSnapshots}")
    private String topic;

    public ProductEventProducer(KafkaTemplate<String, Object> kafka, EventVersionRepository evRepo) {
        this.kafka = kafka;
        this.evRepo = evRepo;
    }

    @Transactional
    public void publish(ProductSnapshot snapshot) {
        String aggregateKey = snapshot.sku();

        // get current version or initialize version
        EventVersion ev = evRepo
                .findById(aggregateKey)
                .orElse(new EventVersion(aggregateKey, 0L));

        long nextVersion = ev.getVersion() + 1;
        ev.setVersion(nextVersion);
        evRepo.save(ev);

        // wrap payload in envelope
        Envelope<ProductSnapshot> envelope = new Envelope<>(
                UUID.randomUUID().toString(),
                EventType.PRODUCT_UPDATED,
                aggregateKey,
                nextVersion,
                LocalDateTime.now(),
                snapshot
        );

        kafka.send(topic, aggregateKey, envelope);
    }
}
