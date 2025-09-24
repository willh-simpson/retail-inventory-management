package com.retail.inventory.inventory_service.domain.model;

import jakarta.persistence.*;

/**
 * holds data for amount of each product on hand held in "products" table and amount to be dispatched
 * from "inventory" table in postgres db
 */
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ensure uniqueness for each entry, not to be confused with Product.id

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // shares id with Product.id

    private int quantityOnHand;
    private int quantityReserved;

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

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }
}
