package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.ProductSnapshotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ProductService extends SnapshotService<ProductSnapshot> {
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
    public ProductSnapshot getProductBySku(String sku) {
        // query local repo first
        Optional<ProductSnapshot> cached = snapshotRepo.findBySku(sku);

        if (cached.isPresent()) {
            return cached.get();
        }

        // fallback to REST query if snapshot is missing
        ProductSnapshot product = rest.getForObject(getBaseUrl() + sku, ProductSnapshot.class);

        if (product != null) {
            return snapshotRepo.save(product);
        } else {
            return new ProductSnapshot(
                    sku,
                    "",
                    "",
                    0.0,
                    Map.of(),
                    ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
            );
        }
    }

    // fallback if circuit breaker is OPEN or retries fail
    @Override
    public ProductSnapshot getFallback(String sku, Throwable t) {
        try {
            return snapshotRepo.findBySku(sku)
                    .orElse(new ProductSnapshot(
                            sku, "",
                            "",
                            0.0,
                            Map.of(),
                            ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion()
                    ));
        } catch (Exception e) {
            return new ProductSnapshot(
                    sku, "",
                    "",
                    0.0,
                    Map.of(),
                    ExceptionVersion.SERVICE_FAILURE.getVersion()
            );
        }
    }
}
