package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.ProductSnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProductService extends SnapshotService<ProductSnapshotEntity> {
    private final ProductSnapshotRepository snapshotRepo;

    public ProductService(RestTemplateBuilder builder, ProductSnapshotRepository snapshotRepo) {
        super(builder);
        this.snapshotRepo = snapshotRepo;
    }

    @Override
    protected String getBaseUrl() {
        return super.getBaseUrl() + "products/";
    }

    @Retry(name = "productServiceRetry")
    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "getFallback")
    public ProductSnapshotEntity getProductBySku(String sku) {
        return rest.getForObject(getBaseUrl() + sku, ProductSnapshotEntity.class);
    }

    // fallback if circuit breaker is OPEN or retries fail
    @Override
    public ProductSnapshotEntity getFallback(String sku, Throwable t) {
        try {
            return snapshotRepo.findBySku(sku)
                    .orElse(new ProductSnapshotEntity(
                            sku, "",
                            "",
                            0.0,
                            Map.of(),
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new ProductSnapshotEntity(
                    sku, "",
                    "",
                    0.0,
                    Map.of(),
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
