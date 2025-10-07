package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshotEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class InventoryService {
    private final RestTemplate rest;

    public InventoryService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    @Retry(name = "inventoryServiceRetry")
    @CircuitBreaker(name = "inventoryServiceCircuitBreaker", fallbackMethod = "getInventoryFallback")
    public InventorySnapshotEntity getInventoryByProductSku(String productSku) {
        return rest.getForObject("http://inventory-service/api/inventory/" + productSku, InventorySnapshotEntity.class);
    }

    // fallback if circuit breaker is OPEN or retries fail
    public InventorySnapshotEntity getInventoryFallback(String productSku, Throwable t) {
        return new InventorySnapshotEntity(productSku, 0, "N/A", LocalDateTime.now(), -1L);
    }
}
