package com.retail.inventory.order_service.domain.repository;

import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSnapshotRepository extends MongoRepository<ProductSnapshotEntity, String> {
    Optional<ProductSnapshotEntity> findBySku(String sku);
}
