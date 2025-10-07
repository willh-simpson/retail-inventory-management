package com.retail.inventory.order_service.domain.model.order;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_retry_queue")
public class OrderRetry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Order order;
    private Long retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptedAt;

    public OrderRetry() {

    }

    public OrderRetry(Order order, LocalDateTime createdAt) {
        this.order = order;
        this.createdAt = createdAt;
        this.lastAttemptedAt = createdAt; // prevent field from being null
        this.retryCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public void incrementCount() {
        retryCount++;
        lastAttemptedAt = LocalDateTime.now();
    }
}
