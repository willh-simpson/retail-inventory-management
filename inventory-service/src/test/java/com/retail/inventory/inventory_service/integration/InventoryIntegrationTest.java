package com.retail.inventory.inventory_service.integration;

import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import com.retail.inventory.inventory_service.domain.repository.InventoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InventoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InventoryRepository invRepo;
    @Autowired
    private ProductRepository proRepo;
    @Autowired
    private CategoryRepository catRepo;

    @BeforeEach
    void resetSequences() {
        jdbc.execute("TRUNCATE TABLE inventory RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE categories RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testAddAndGetInventory() throws Exception {
        // create category and product that have to exist before inventory can be created
        Category cat = new Category("test category", "desc");
        catRepo.save(cat);
        Product pro = new Product("12345678", "test", "desc", 9.99, cat);
        proRepo.save(pro);
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);

        // id must be left out of request.body so JPA knows this is a POST and not a PUT
        String requestBody = """
                {
                    "product_id": %d,
                    "quantity": 5,
                    "location": "aisle 1",
                    "last_updated": "%s"
                }
                """.formatted(
                        pro.getId(),
                        currentTime.toString()
                    );

        // verify inventory info was added to db
        mvc.perform(post("/api/inventory")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.product_id").value(pro.getId()))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.location").value("aisle 1"))
                .andExpect(jsonPath("$.last_updated").value("2025-09-25T10:30:00"));

        // verify inventory item exists in db
        mvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.product_id").value(pro.getId()))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetAllInventory() throws Exception {
        Category cat = new Category("test", "desc");
        catRepo.save(cat);
        Product product1 = new Product("12345678", "test 1", "desc", 9.99, cat);
        proRepo.save(product1);
        Product product2 = new Product("00010001", "test 2", "desc", 19.99, cat);
        proRepo.save(product2);
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);

        InventoryItem inv1 = new InventoryItem(product1, 5, "aisle 1", currentTime);
        InventoryItem savedInv1 = invRepo.save(inv1);
        InventoryItem inv2 = new InventoryItem(product2, 3, "aisle 2", currentTime);
        InventoryItem savedInv2 = invRepo.save(inv2);

        // verify all inventory items are returned by db
        mvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedInv1.getId()))
                .andExpect(jsonPath("$[0].product_id").value(savedInv1.getProduct().getId()))
                .andExpect(jsonPath("$[0].quantity").value(savedInv1.getQuantity()))
                .andExpect(jsonPath("$[0].location").value(savedInv1.getLocation()))
                .andExpect(jsonPath("$[1].id").value(savedInv2.getId()))
                .andExpect(jsonPath("$[1].product_id").value(savedInv2.getProduct().getId()))
                .andExpect(jsonPath("$[1].quantity").value(savedInv2.getQuantity()))
                .andExpect(jsonPath("$[1].location").value(savedInv2.getLocation()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testAddStock() throws Exception {
        Category cat = new Category("test category", "desc");
        catRepo.save(cat);
        Product pro = new Product("12345678", "test", "desc", 9.99, cat);
        proRepo.save(pro);
        LocalDateTime currentTime = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
        InventoryItem inv = new InventoryItem(pro, 5, "aisle 1", currentTime);
        InventoryItem savedInv = invRepo.save(inv);

        String requestBody = """
                {
                    "quantity": 5
                }
                """;

        // verify stock was increased by 5 totalling 10
        mvc.perform(put("/api/inventory/{product_id}/stock", savedInv.getProduct().getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedInv.getId()))
                .andExpect(jsonPath("$.product_id").value(savedInv.getProduct().getId()))
                .andExpect(jsonPath("$.quantity").value(10));

        // verify db was updated by checking db after PUT request
        InventoryItem updatedInv = invRepo.findById(savedInv.getId()).orElseThrow(() -> new RuntimeException("Inventory item not found"));
        assertThat(updatedInv.getQuantity()).isEqualTo(10);
    }
}
