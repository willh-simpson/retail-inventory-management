package com.retail.inventory.inventory_service.integration;

import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import com.retail.inventory.inventory_service.infrastructure.messaging.CategoryEventProducer;
import com.retail.inventory.inventory_service.infrastructure.messaging.InventoryEventProducer;
import com.retail.inventory.inventory_service.infrastructure.messaging.ProductEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository proRepo;
    @Autowired
    private CategoryRepository catRepo;

    @MockBean
    private CategoryEventProducer catEventProducer;
    @MockBean
    private InventoryEventProducer invEventProducer;
    @MockBean
    private ProductEventProducer proEventProducer;

    @BeforeEach
    void resetSequences() {
        jdbc.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE categories RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testAddAndGetProduct() throws Exception {
        // category has to exist already before a product can be created
        Category cat = new Category("test category", "desc");
        Category savedCat = catRepo.save(cat);

        // id must be left out of request.body so JPA knows this is a POST and not a PUT
        String requestBody = """
                {
                    "sku": "12345678",
                    "name": "test",
                    "description": "desc",
                    "price": 9.99,
                    "category_id": %d
                }
                """.formatted(savedCat.getId());

        // verify product was added to db
        mvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L)) // verify JPA generated the proper serial primary key
                .andExpect(jsonPath("$.sku").value("12345678"))
                .andExpect(jsonPath("$.name").value("test"));

        // verify product exists in db
        mvc.perform(get("/api/products/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("12345678"))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.price").value(9.99));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetAllProducts() throws Exception {
        // create a list of products to save to repo
        Category cat = new Category("test", "desc");
        catRepo.save(cat);
        Product product1 = new Product("12345678", "test 1", "desc", 9.99, cat);
        Product savedPro1 = proRepo.save(product1);
        Product product2 = new Product("00010001", "test 2", "desc", 19.99, cat);
        Product savedPro2 = proRepo.save(product2);

        // verify all products are returned by db
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedPro1.getId()))
                .andExpect(jsonPath("$[0].sku").value(savedPro1.getSku()))
                .andExpect(jsonPath("$[1].id").value(savedPro2.getId()))
                .andExpect(jsonPath("$[1].sku").value(savedPro2.getSku()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testUpdatePrice() throws Exception {
        Category cat = new Category("test category", "desc");
        Category savedCat = catRepo.save(cat);

        Product product = new Product("12345678", "test", "desc", 9.99, savedCat);
        Product savedProduct = proRepo.save(product);

        String requestBody = """
                {
                    "price": 19.99
                }
                """;

        // verify product was updated in db
        mvc.perform(put("/api/products/{id}/price", savedProduct.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.sku").value(savedProduct.getSku()))
                .andExpect(jsonPath("$.name").value(savedProduct.getName()))
                .andExpect(jsonPath("$.price").value(19.99));

        // verify db was updated by checking db after PUT request
        Product updatedProduct = proRepo.findById(savedProduct.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
        assertThat(updatedProduct.getPrice()).isEqualTo(19.99);
    }
}
