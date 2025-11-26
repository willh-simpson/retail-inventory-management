package com.retail.inventory.inventory_service.api;

import com.retail.inventory.inventory_service.api.controller.InventoryController;
import com.retail.inventory.inventory_service.api.dto.request.InventoryRequestDto;
import com.retail.inventory.inventory_service.application.service.InventoryService;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import com.retail.inventory.inventory_service.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class InventoryControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private InventoryService service;

    @Test
    void testAddInventoryItem() throws Exception {
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        InventoryItem item = new InventoryItem(1L, product, 5, "aisle 1", currentTime);

        // mock service behavior
        when(service.addInventoryItem(any(InventoryRequestDto.class))).thenReturn(item);

        String requestBody = """
                {
                    "id": 1,
                    "product_id": 1,
                    "quantity": 5,
                    "location": "aisle 1",
                    "last_updated": "2025-09-25T10:30:00"
                }
                """;

        mvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.product_id").value(1L))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void testGetInventoryItem() throws Exception {
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        InventoryItem item = new InventoryItem(1L, product, 5, "aisle 1", currentTime);

        // mock service behavior
        when(service.getInventoryItem(product.getId())).thenReturn(item);

        mvc.perform(get("/api/inventory/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.product_id").value(1L))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void testGetAllInventory() throws Exception {
        // mock list of 2 inventory items
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
        Category cat1 = new Category(1L, "example", "example desc");
        Product product1 = new Product(1L, "00589837", "example", "example desc", 9.99, cat1);
        InventoryItem inv1 = new InventoryItem(1L, product1, 5, "aisle 1", currentTime);

        Category cat2 = new Category(2L, "example 2", "example desc");
        Product product2 = new Product(2L, "00010001", "example 2", "example desc", 19.99, cat2);
        InventoryItem inv2 = new InventoryItem(2L, product2, 3, "aisle 2", currentTime);

        List<InventoryItem> items = Arrays.asList(inv1, inv2);

        // mock service behavior
        when(service.getAllInventory()).thenReturn(items);

        mvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testAddStock() throws Exception {
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        InventoryItem item = new InventoryItem(1L, product, 5, "aisle 1", currentTime);

        // mock service behavior
        when(service.addStock(item.getProduct().getId(), 5)).thenReturn(new InventoryItem(1L, product, 10, "aisle 1", currentTime));

        String requestBody = """
                {
                    "quantity": 5
                }
                """;

        mvc.perform(put("/api/inventory/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.quantity").value(10));
    }
}
