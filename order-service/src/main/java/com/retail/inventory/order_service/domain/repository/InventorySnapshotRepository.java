package com.retail.inventory.order_service.domain.repository;

import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshotEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventorySnapshotRepository extends MongoRepository<InventorySnapshotEntity, String> {
    Optional<InventorySnapshotEntity> findByProductSku(String productSku);
}
