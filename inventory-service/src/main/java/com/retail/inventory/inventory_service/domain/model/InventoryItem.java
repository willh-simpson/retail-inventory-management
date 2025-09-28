package com.retail.inventory.inventory_service.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * holds data for amount of each product on hand held in "products" table and amount to be dispatched
 * from "inventory" table in postgres db
 */
@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ensure uniqueness for each entry, not to be confused with Product.id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // shares id with Product.id

    private int quantity;
    private String location;
    private LocalDateTime lastUpdated;

    public InventoryItem() {

    }

    public InventoryItem(Long id, Product product, int quantity, String location, LocalDateTime lastUpdated) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.location = location;
        this.lastUpdated = lastUpdated;
    }

    public InventoryItem(Product product, int quantity, String location, LocalDateTime lastUpdated) {
        this.product = product;
        this.quantity = quantity;
        this.location = location;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
}
