package com.retail.inventory.inventory_service.application;

import com.retail.inventory.inventory_service.api.dto.ProductRequestDto;
import com.retail.inventory.inventory_service.application.service.ProductService;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
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
class ProductServiceTest {
    @Mock
    private ProductRepository productRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @InjectMocks
    private ProductService service;

    @Test
    void testAddProduct() {
        // create example product and set all of its properties
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product();
        product.setId(1L);
        product.setSku("00589837");
        product.setName("example");
        product.setPrice(9.99);
        product.setCategory(cat);

        // mock repo save()
        when(productRepo.save(any(Product.class))).thenReturn(product);

        // mock category repo find()
        when(categoryRepo.findById(cat.getId())).thenReturn(Optional.of(cat));

        // verify the correct product was added
        Product savedProduct = service.addProduct(new ProductRequestDto(1L, "00589837", "example", "example desc", 9.99, 1L));
        assertThat(savedProduct.getId()).isEqualTo(1L);
        assertThat(savedProduct.getSku()).isEqualTo("00589837");
        assertThat(savedProduct.getName()).isEqualTo("example");
        assertThat(savedProduct.getPrice()).isEqualTo(9.99);

        // verify repo functions were called only once
        verify(productRepo, times(1)).save(any(Product.class));
        verify(categoryRepo, times(1)).findById(cat.getId());
    }

    @Test
    void testFindBySku_Success() {
        // create example product
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(2L, "00010001", "example 2", "example desc", 19.99, cat);

        // mock repo find()
        when(productRepo.findBySku("00010001")).thenReturn(Optional.of(product));

        // verify product was found
        Product foundProduct = service.getProduct("00010001");
        assertThat(foundProduct.getSku()).isEqualTo("00010001");

        // verify repo was called only once
        verify(productRepo, times(1)).findBySku("00010001");
    }

    @Test
    void testFindBySku_Fail() {
        // mock a failed get request
        when(productRepo.findBySku("00020002")).thenReturn(Optional.empty());

        // verify exception was thrown when calling controller
        assertThatThrownBy(() -> service.getProduct("00020002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }

    @Test
    void testGetAllProducts() {
        // populate a list with mock products
        Category cat1 = new Category(1L, "example", "example desc");
        Product product1 = new Product(1L, "00589837", "example", "example desc", 9.99, cat1);

        Category cat2 = new Category(2L, "example 2", "example desc");
        Product product2 = new Product(2L, "00010001", "example 2", "example desc", 19.99, cat2);

        List<Product> products = Arrays.asList(product1, product2);

        // mock repo findAll()
        when(productRepo.findAll()).thenReturn(products);

        // verify controller is getting all products
        List<Product> foundProducts = service.getAllProducts();
        ListAssert<Product> listAssert = org.assertj.core.api.Assertions.assertThat(foundProducts);
        listAssert.hasSize(2);
        listAssert.extracting(Product::getSku).containsExactly("00589837", "00010001");

        // verify repo was called only once
        verify(productRepo, times(1)).findAll();
    }

    @Test
    void testUpdatePrice() {
        // create a product with base price
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);

        // mock repo find()
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        // mock repo save()
        when(productRepo.save(product)).thenAnswer(i -> i.getArgument(0));

        // verify updated price is now 10.99
        Product newPrice = service.updatePrice(product.getId(), 10.99);
        assertThat(newPrice.getPrice()).isEqualTo(10.99);

        // verify repo functions were called only once
        verify(productRepo, times(1)).findById(product.getId());
        verify(productRepo, times(1)).save(any(Product.class));
    }
}
