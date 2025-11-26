package com.retail.inventory.order_service.domain.model.snapshot;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("inventory_snapshot")
public class InventorySnapshot {
    @Id
    private String id;

    private String productSku;
    private int quantity;
    private String location;
    private LocalDateTime lastUpdated;

    private final Long version;

    public InventorySnapshot(String productSku, int quantity, String location, LocalDateTime lastUpdated, Long version) {
        this.productSku = productSku;
        this.quantity = quantity;
        this.location = location;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }
}
