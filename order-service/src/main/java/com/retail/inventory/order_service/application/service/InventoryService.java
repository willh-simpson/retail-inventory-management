package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.InventorySnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InventoryService extends SnapshotService<InventorySnapshot> {
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
    public InventorySnapshot getInventoryByProductSku(String productSku) {
        // query local repo first
        Optional<InventorySnapshot> cached = snapshotRepo.findByProductSku(productSku);

        if (cached.isPresent()) {
            return cached.get();
        }

        // fallback to REST query if snapshot is missing
        InventorySnapshot inventory = rest.getForObject(getBaseUrl() + productSku, InventorySnapshot.class);

        if (inventory != null) {
            return snapshotRepo.save(inventory);
        } else {
            return new InventorySnapshot(
                    productSku,
                    0,
                    "",
                    LocalDateTime.now(),
                    ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
            );
        }
    }

    // fallback if circuit breaker is OPEN or retries fail
    @Override
    public InventorySnapshot getFallback(String productSku, Throwable t) {
        try {
            return snapshotRepo.findByProductSku(productSku)
                    .orElse(new InventorySnapshot(
                            productSku,
                            0,
                            "",
                            LocalDateTime.now(),
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new InventorySnapshot(
                    productSku,
                    0,
                    "",
                    LocalDateTime.now(),
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
