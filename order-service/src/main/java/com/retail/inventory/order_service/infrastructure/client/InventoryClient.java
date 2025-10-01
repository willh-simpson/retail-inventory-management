package com.retail.inventory.order_service.infrastructure.client;

import com.retail.inventory.order_service.api.dto.request.ReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "http://localhost:8081/api/inventory")
public interface InventoryClient {
    @PutMapping("/reserve")
    boolean reserve(@RequestBody ReserveRequest req);
}
