package com.retail.inventory.inventory_service.domain.repository;

import com.retail.inventory.inventory_service.domain.model.EventVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventVersionRepository extends JpaRepository<EventVersion, String> {
}
