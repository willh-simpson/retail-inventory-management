package com.retail.inventory.order_service.domain.model;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document("product_snapshot")
public class ProductSnapshotEntity {
    @Id
    private String sku;

    private String name;
    private String description;
    private double price;
    private Map<String, Object> attributes;

    private final Long version;

    public ProductSnapshotEntity(String sku, String name, String description, double price, Map<String, Object> attributes, Long version) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.attributes = attributes;
        this.version = version;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Long getVersion() {
        return version;
    }
}
