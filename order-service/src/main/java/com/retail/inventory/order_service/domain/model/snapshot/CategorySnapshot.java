package com.retail.inventory.order_service.domain.model.snapshot;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("category_snapshot")
public class CategorySnapshot {
    @Id
    private String id;

    private String name;
    private String description;

    private final Long version;

    public CategorySnapshot(String name, String description, Long version) {
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
