package com.retail.inventory.inventory_service.api;

import com.retail.inventory.inventory_service.api.controller.CategoryController;
import com.retail.inventory.inventory_service.api.dto.request.CategoryRequestDto;
import com.retail.inventory.inventory_service.application.service.CategoryService;
import com.retail.inventory.inventory_service.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoryService service;

    @Test
    void testAddCategory() throws Exception {
        Category cat = new Category(1L, "example", "example desc");

        // mock service behavior
        when(service.addCategory(any(CategoryRequestDto.class))).thenReturn(cat);

        String requestBody = """
                {
                    "id": 1,
                    "name": "example",
                    "description": "example desc"
                }
                """;

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("example"))
                .andExpect(jsonPath("$.description").value("example desc"));
    }

    @Test
    void getGetCategoryByName() throws Exception {
        Category cat = new Category(1L, "example", "example desc");

        // mock service behavior
        when(service.getCategory("example")).thenReturn(cat);

        mvc.perform(get("/api/categories/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("example"));
    }

    @Test
    void testGetAllCategories() throws Exception {
        // mock list of all categories
        Category cat1 = new Category(1L, "example", "example desc");
        Category cat2 = new Category(2L, "example 2", "example desc");
        List<Category> categories = Arrays.asList(cat1, cat2);

        // mock service behavior
        when(service.getAllCategories()).thenReturn(categories);

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testChangeName() throws Exception {
        Category cat = new Category(1L, "example", "example desc");

        // mock service behavior
        when(service.changeName(cat.getId(), "new name")).thenReturn(new Category(1L, "new name", "example desc"));

        String requestBody = """
                {
                    "name": "new name"
                }
                """;

        mvc.perform(put("/api/categories/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("new name"));
    }
}
