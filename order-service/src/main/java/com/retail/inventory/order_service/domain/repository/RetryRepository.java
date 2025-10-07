package com.retail.inventory.order_service.domain.repository;

import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetryRepository extends JpaRepository<OrderRetry, Long> {
}
