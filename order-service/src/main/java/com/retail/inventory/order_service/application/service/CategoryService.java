package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshot;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.repository.snapshot.CategorySnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CategoryService extends SnapshotService<CategorySnapshot> {
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
    public CategorySnapshot getCategoryByName(String name) {
        // query local repo first
        Optional<CategorySnapshot> cached = snapshotRepo.findByName(name);

        if (cached.isPresent()) {
            return cached.get();
        }

        // fallback to REST query if snapshot is missing
        CategorySnapshot category = rest.getForObject(getBaseUrl() + name, CategorySnapshot.class);

        if (category != null) {
            return snapshotRepo.save(category);
        } else {
            return new CategorySnapshot(
                    name,
                    "",
                    ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
            );
        }
    }

    @Override
    public CategorySnapshot getFallback(String name, Throwable t) {
        try {
            return snapshotRepo.findByName(name)
                    .orElse(new CategorySnapshot(
                            name,
                            "",
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new CategorySnapshot(
                    name,
                    "",
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
