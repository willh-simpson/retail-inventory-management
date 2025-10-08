package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshotEntity;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.repository.CategorySnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

@Service
public class CategoryService extends SnapshotService<CategorySnapshotEntity> {
    private final CategorySnapshotRepository snapshotRepo;

    public CategoryService(RestTemplateBuilder builder, CategorySnapshotRepository snapshotRepo) {
        super(builder);
        this.snapshotRepo = snapshotRepo;
    }

    @Override
    protected String getBaseUrl() {
        return super.getBaseUrl() + "categories/";
    }

    @Retry(name = "categoryServiceRetry")
    @CircuitBreaker(name = "categoryServiceCircuitBreaker", fallbackMethod = "getFallback")
    public CategorySnapshotEntity getCategoryByName(String name) {
        return rest.getForObject(getBaseUrl() + name, CategorySnapshotEntity.class);
    }

    @Override
    public CategorySnapshotEntity getFallback(String name, Throwable t) {
        try {
            return snapshotRepo.findByName(name)
                    .orElse(new CategorySnapshotEntity(
                            "",
                            "",
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new CategorySnapshotEntity(
                    "",
                    "",
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
