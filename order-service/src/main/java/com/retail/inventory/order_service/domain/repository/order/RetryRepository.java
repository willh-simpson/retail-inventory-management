package com.retail.inventory.order_service.domain.repository.order;

import com.retail.inventory.order_service.domain.model.order.Order;
import com.retail.inventory.order_service.domain.model.order.OrderRetry;
import com.retail.inventory.order_service.domain.model.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetryRepository extends JpaRepository<OrderRetry, Long> {
}
