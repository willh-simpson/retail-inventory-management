package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshotEntity;
import com.retail.inventory.order_service.domain.repository.InventorySnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryService extends SnapshotService<InventorySnapshotEntity> {
    private final InventorySnapshotRepository snapshotRepo;

    public InventoryService(RestTemplateBuilder builder, InventorySnapshotRepository snapshotRepo) {
        super(builder);
        this.snapshotRepo = snapshotRepo;
    }

    @Override
    protected String getBaseUrl() {
        return super.getBaseUrl() + "inventory/";
    }

    @Retry(name = "inventoryServiceRetry")
    @CircuitBreaker(name = "inventoryServiceCircuitBreaker", fallbackMethod = "getFallback")
    public InventorySnapshotEntity getInventoryByProductSku(String productSku) {
        return rest.getForObject(getBaseUrl() + productSku, InventorySnapshotEntity.class);
    }

    // fallback if circuit breaker is OPEN or retries fail
    @Override
    public InventorySnapshotEntity getFallback(String productSku, Throwable t) {
        try {
            return snapshotRepo.findByProductSku(productSku)
                    .orElse(new InventorySnapshotEntity(
                            productSku,
                            0,
                            "",
                            LocalDateTime.now(),
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new InventorySnapshotEntity(
                    productSku,
                    0,
                    "",
                    LocalDateTime.now(),
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
