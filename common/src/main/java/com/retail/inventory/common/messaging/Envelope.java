package com.retail.inventory.common.messaging;

import com.retail.inventory.common.messaging.model.EventType;

import java.time.LocalDateTime;

public class Envelope<T> {
    private String eventId;
    private EventType eventType;
    private String aggregateKey; // sku or category name
    private Long version;
    private LocalDateTime timestamp;
    private T payload;

    public Envelope() {

    }

    public Envelope(String eventId, EventType eventType, String aggregateKey, Long version, LocalDateTime timestamp, T payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateKey = aggregateKey;
        this.version = version;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getAggregateKey() {
        return aggregateKey;
    }

    public void setAggregateKey(String aggregateKey) {
        this.aggregateKey = aggregateKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
