package com.retail.inventory.order_service.domain.repository;

import com.retail.inventory.order_service.domain.model.CategorySnapshotEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategorySnapshotRepository extends MongoRepository<CategorySnapshotEntity, String> {
    Optional<CategorySnapshotEntity> findByName(String name);
}
