package com.retail.inventory.inventory_service.api.controller;

import com.retail.inventory.inventory_service.api.dto.request.InventoryReservationRequest;
import com.retail.inventory.inventory_service.api.dto.response.InventoryItemDto;
import com.retail.inventory.inventory_service.api.dto.request.InventoryRequestDto;
import com.retail.inventory.inventory_service.api.dto.request.StockRequest;
import com.retail.inventory.inventory_service.api.dto.response.InventoryReservationResponse;
import com.retail.inventory.inventory_service.application.service.InventoryService;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * implement application to server connection with "inventory" table in postgres db
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InventoryItemDto> addInventoryItem(@RequestBody InventoryRequestDto req) {
        InventoryItem item = service.addInventoryItem(req);

        // POST request needs to return HTTP status 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(InventoryItemDto.fromEntity(item));
    }

    @GetMapping("/{product_id}")
    public ResponseEntity<InventoryItemDto> getInventoryItem(@PathVariable("product_id") Long productId) {
        InventoryItem item = service.getInventoryItem(productId);

        // ok() returns HTTP status 200
        return ResponseEntity.ok(InventoryItemDto.fromEntity(item));
    }

    @GetMapping("/{product_sku}")
    public ResponseEntity<InventoryItemDto> getInventoryItemBySku(@PathVariable("product_sku") String productSku) {
        InventoryItem item = service.getInventoryItemBySku(productSku);

        return ResponseEntity.ok(InventoryItemDto.fromEntity(item));
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemDto>> getAllInventory() {
        List<InventoryItem> items = service.getAllInventory();

        return ResponseEntity.ok(InventoryItemDto.fromEntityList(items));
    }

    @PutMapping("/{product_id}/stock")
    public ResponseEntity<InventoryItemDto> addStock(@PathVariable("product_id") Long productId, @RequestBody StockRequest req) {
        InventoryItem item = service.addStock(productId, req.quantity());

        return ResponseEntity.ok(InventoryItemDto.fromEntity(item));
    }

    @PutMapping("/reserve")
    @Transactional
    public ResponseEntity<InventoryReservationResponse> reserveInventory(@RequestBody InventoryReservationRequest req) {
        InventoryItem item = service.getInventoryItem(req.productId());

        if (item.getQuantity() < req.quantity()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new InventoryReservationResponse(false, "Insufficient stock"));
        }

        // subtract stock using addStock()
        addStock(item.getProduct().getId(), new StockRequest(-req.quantity()));

        return ResponseEntity.ok(new InventoryReservationResponse(true, "Reserved"));
    }
}
