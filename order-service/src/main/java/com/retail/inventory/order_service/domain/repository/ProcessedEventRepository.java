package com.retail.inventory.order_service.domain.repository;

import com.retail.inventory.order_service.domain.model.event.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
