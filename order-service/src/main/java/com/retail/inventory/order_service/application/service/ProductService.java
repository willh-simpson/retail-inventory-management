package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ProductService {
    private final RestTemplate rest;

    public ProductService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    @Retry(name = "productServiceRetry")
    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "getProductFallback")
    public ProductSnapshotEntity getProductBySku(String sku) {
        return rest.getForObject("http://inventory-service/api/products/" + sku, ProductSnapshotEntity.class);
    }

    // fallback if circuit breaker is OPEN or retries fail
    public ProductSnapshotEntity getProductFallback(String sku, Throwable t) {
        return new ProductSnapshotEntity(sku, "Unavailable", "Product service is down", 0.0, Map.of(), -1L);
    }
}
