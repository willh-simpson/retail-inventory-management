package com.retail.inventory.order_service.domain.repository.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventorySnapshotRepository extends MongoRepository<InventorySnapshot, String> {
    Optional<InventorySnapshot> findByProductSku(String productSku);
}
