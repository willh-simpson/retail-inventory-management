package com.retail.inventory.order_service.application.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

public abstract class SnapshotService<T> {
    protected final RestTemplate rest;

    protected SnapshotService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    protected String getBaseUrl() {
        return "http://inventory-service/api/";
    }

    protected abstract T getFallback(String key, Throwable t);
}
