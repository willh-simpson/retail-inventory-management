package com.retail.inventory.order_service.application.service;

import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshotEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CategoryService {
    private final RestTemplate rest;

    public CategoryService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    @Retry(name = "categoryServiceRetry")
    @CircuitBreaker(name = "categoryServiceCircuitBreaker", fallbackMethod = "getCategoryFallback")
    public CategorySnapshotEntity getCategoryByName(String name) {
        return rest.getForObject("http://inventory-service/api/categories/" + name, CategorySnapshotEntity.class);
    }

    public CategorySnapshotEntity getCategoryFallback(String name, Throwable t) {
        return new CategorySnapshotEntity("Unavailable", "Category service down", -1L);
    }
}
