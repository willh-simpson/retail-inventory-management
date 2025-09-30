package com.retail.inventory.order_service.domain.model;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("category_snapshot")
public class CategorySnapshotEntity {
    @Id
    private String name;

    private String description;

    private final Long version;

    public CategorySnapshotEntity(String name, String description, Long version) {
        this.name = name;
        this.description = description;
        this.version = version;
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

    public Long getVersion() {
        return version;
    }
}
