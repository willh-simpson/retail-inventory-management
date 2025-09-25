package com.retail.inventory.inventory_service.api;

import com.retail.inventory.inventory_service.api.controller.ProductController;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.assertj.core.api.ListAssert;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock
    private ProductRepository repo;

    @InjectMocks
    private ProductController controller;

    @Test
    void testAddProduct() {
        // create example product and set all of its properties
        Product product = new Product();
        product.setId(1L);
        product.setSku("00589837");
        product.setName("example");
        product.setPrice(9.99);

        // mock repo save()
        when(repo.save(any(Product.class))).thenReturn(product);

        // verify the correct product was added
        Product savedProduct = controller.addProduct(new Product("00589837", "example", 9.99));
        assertThat(savedProduct.getId()).isEqualTo(1L);
        assertThat(savedProduct.getSku()).isEqualTo("00589837");
        assertThat(savedProduct.getName()).isEqualTo("example");
        assertThat(savedProduct.getPrice()).isEqualTo(9.99);

        // verify repo was called only once
        verify(repo, times(1)).save(any(Product.class));
    }

    @Test
    void testFindBySku_Success() {
        // create example product
        Product product = new Product("00010001", "example 2", 19.99);
        product.setId(2L);

        // mock repo find()
        when(repo.findBySku("00010001")).thenReturn(Optional.of(product));

        // verify product was found
        Product foundProduct = controller.getProduct("00010001");
        assertThat(foundProduct.getSku()).isEqualTo("00010001");

        // verify repo was called only once
        verify(repo, times(1)).findBySku("00010001");
    }

    @Test
    void testFindBySku_Fail() {
        // mock a failed get request
        when(repo.findBySku("00020002")).thenReturn(Optional.empty());

        // verify exception was thrown when calling controller
        assertThatThrownBy(() -> controller.getProduct("00020002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }

    @Test
    void testGetAllProducts() {
        // populate a list with mock products
        Product product1 = new Product("00589837", "example", 9.99);
        product1.setId(1L);

        Product product2 = new Product("00010001", "example 2", 19.99);
        product2.setId(2L);

        List<Product> products = Arrays.asList(product1, product2);

        // mock repo findAll()
        when(repo.findAll()).thenReturn(products);

        // verify controller is getting all products
        List<Product> foundProducts = controller.getAllProducts();
        ListAssert<Product> listAssert = org.assertj.core.api.Assertions.assertThat(foundProducts);
        listAssert.hasSize(2);
        listAssert.extracting(Product::getSku).containsExactly("00589837", "00010001");

        // verify repo was called only once
        verify(repo, times(1)).findAll();
    }
}
