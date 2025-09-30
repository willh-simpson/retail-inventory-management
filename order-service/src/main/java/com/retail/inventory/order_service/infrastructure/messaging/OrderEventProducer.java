package com.retail.inventory.order_service.infrastructure.messaging;

import com.retail.inventory.order_service.api.dto.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderCreatedEvent> kafka;

    public OrderEventProducer(KafkaTemplate<String, OrderCreatedEvent> kafka) {
        this.kafka = kafka;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafka.send("order.created", event);
    }

    @KafkaListener(topics = "order.created", groupId = "inventory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // try to reserve inventory (idempotent)
        // publish order.inventory_reserved or order.inventory_failed
    }
}
