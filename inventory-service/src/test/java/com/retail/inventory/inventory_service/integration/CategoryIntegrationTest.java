package com.retail.inventory.inventory_service.integration;

import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
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
public class CategoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;

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
        jdbc.execute("TRUNCATE TABLE categories RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testAddAndGetCategory() throws Exception {
        // id must be left out of request.bod so JPA knows this is a POST and not a PUT
        String requestBody = """
                {
                    "name": "test",
                    "description": "desc"
                }
                """;

        // verify category was added to db
        mvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.description").value("desc"));

        // verify category exists in db
        mvc.perform(get("/api/categories/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.description").value("desc"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetAllCategories() throws Exception {
        Category cat1 = new Category("test 1", "desc");
        Category savedCat1 = catRepo.save(cat1);
        Category cat2 = new Category("test 2", "desc");
        Category savedCat2 = catRepo.save(cat2);

        // verify all categories are returned by db
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedCat1.getId()))
                .andExpect(jsonPath("$[0].name").value(savedCat1.getName()))
                .andExpect(jsonPath("$[1].id").value(savedCat2.getId()))
                .andExpect(jsonPath("$[1].name").value(savedCat2.getName()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testChangeName() throws Exception {
        Category cat = new Category("test", "desc");
        Category savedCat = catRepo.save(cat);

        String requestBody = """
                {
                    "name": "new name"
                }
                """;

        // verify category name was changed in db
        mvc.perform(put("/api/categories/{id}/name", savedCat.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCat.getId()))
                .andExpect(jsonPath("$.name").value("new name"));

        // verify db was updated by checking db after PUT request
        Category updatedCat = catRepo.findById(savedCat.getId()).orElseThrow(() -> new RuntimeException("Category not found"));
        assertThat(updatedCat.getName()).isEqualTo("new name");
    }
}
