package com.retail.inventory.inventory_service.api;

import com.retail.inventory.inventory_service.api.controller.ProductController;
import com.retail.inventory.inventory_service.api.dto.request.ProductRequestDto;
import com.retail.inventory.inventory_service.application.service.ProductService;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.Product;
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

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductService service;

    @Test
    void testAddProduct() throws Exception {
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);

        // mock service behavior
        when(service.addProduct(any(ProductRequestDto.class))).thenReturn(product);

        String requestBody = """
                {
                    "id": 1,
                    "sku": "00589837",
                    "name": "example",
                    "description": "example desc",
                    "price": 9.99,
                    "category_id": 1
                }
                """;

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("00589837"));
    }

    @Test
    void testGetProductBySku() throws Exception {
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);

        // mock service behavior
        when(service.getProduct("00589837")).thenReturn(product);

        mvc.perform(get("/api/products/00589837"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("00589837"));
    }

    @Test
    void testGetAllProducts() throws Exception {
        // create mock list of all products
        Category cat = new Category(1L, "example", "example desc");
        Product product1 = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        Product product2 = new Product(2L, "00010001", "example 2", "example desc", 19.99, cat);
        List<Product> products = Arrays.asList(product1, product2);

        // mock service behavior
        when(service.getAllProducts()).thenReturn(products);

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L)) // check products 1L and 2L were found
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$.length()").value(2)); // check correct amount of products was returned
    }

    @Test
    void testUpdatePrice() throws Exception {
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);

        // mock service behavior
        when(service.updatePrice(product.getId(), 10.99)).thenReturn(new Product(1L, "00589837", "example", "example desc", 10.99, cat));

        String requestBody = """
                {
                    "price": 10.99
                }
                """;

        mvc.perform(put("/api/products/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.price").value(10.99));
    }
}
