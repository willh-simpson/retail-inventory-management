package com.retail.inventory.order_service.domain.repository.snapshot;

import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategorySnapshotRepository extends MongoRepository<CategorySnapshot, String> {
    Optional<CategorySnapshot> findByName(String name);
}
