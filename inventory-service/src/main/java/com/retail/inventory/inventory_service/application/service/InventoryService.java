package com.retail.inventory.inventory_service.application.service;

import com.retail.inventory.inventory_service.api.dto.request.InventoryRequestDto;
import com.retail.inventory.inventory_service.api.dto.snapshot.InventorySnapshot;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.InventoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import com.retail.inventory.inventory_service.infrastructure.messaging.InventoryEventProducer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * implement application to server connection with "inventory" table in postgres db
 */
@Service
public class InventoryService {
    private final InventoryRepository invRepo;
    private final ProductRepository productRepo;

    private final InventoryEventProducer eventProducer;

    public InventoryService(InventoryRepository invRepo, ProductRepository productRepo, InventoryEventProducer invEvent) {
        this.invRepo = invRepo;
        this.productRepo = productRepo;
        this.eventProducer = invEvent;
    }

    private InventoryItem fromRequestDto(InventoryRequestDto req) {
        Product product = productRepo
                .findById(req.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return new InventoryItem(req.id(), product, req.quantity(), req.location(), req.lastUpdated());
    }

    private InventoryItem publishInventoryEvent(InventoryItem item) {
        eventProducer.publish(InventorySnapshot.fromEntity(item));

        return item;
    }

    /**
     * return inventory information for a specific product
     *
     * @param productId product to request inventory information
     * @return matching inventory or throw error if not found
     */
    public InventoryItem getInventoryItem(Long productId) {
        return invRepo
                .findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    }

    /**
     * return inventory information for a specific product via its SKU
     *
     * @param productSku product to request inventory information
     * @return matching inventory or throw error if not found
     */
    public InventoryItem getInventoryItemBySku(String productSku) {
        return invRepo
                .findByProductSku(productSku)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    }

    /**
     * return all inventory information for each product
     *
     * @return list of all inventory for all mapped products
     */
    public List<InventoryItem> getAllInventory() {
        return invRepo.findAll();
    }

    /**
     * add inventory information for a product
     *
     * @param req inventory information to add
     * @return inventory item upon success
     */
    public InventoryItem addInventoryItem(InventoryRequestDto req) {
        InventoryItem item = fromRequestDto(req);
        InventoryItem savedItem = invRepo.save(item);

        return publishInventoryEvent(savedItem);
    }

    /**
     * update product stock
     *
     * @param productId product_id matching products table
     * @param quantity change in quantity
     * @return inventory item upon success
     */
    public InventoryItem addStock(Long productId, int quantity) {
        InventoryItem item = invRepo
                .findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        item.setQuantity(item.getQuantity() + quantity);
        item.setLastUpdated(LocalDateTime.now());

        InventoryItem savedItem = invRepo.save(item);

        return publishInventoryEvent(savedItem);
    }
}
