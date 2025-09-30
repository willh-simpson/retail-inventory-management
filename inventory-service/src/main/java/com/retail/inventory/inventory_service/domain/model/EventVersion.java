package com.retail.inventory.inventory_service.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_versions")
public class EventVersion {
    @Id
    private String aggregateKey;

    private Long version;

    public EventVersion()  {

    }

    public EventVersion(String aggregateKey, Long version) {
        this.aggregateKey = aggregateKey;
        this.version = version;
    }

    public String getAggregateKey() {
        return aggregateKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
