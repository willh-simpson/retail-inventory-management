package com.retail.inventory.order_service.domain.repository.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSnapshotRepository extends MongoRepository<ProductSnapshot, String> {
    Optional<ProductSnapshot> findBySku(String sku);
}
