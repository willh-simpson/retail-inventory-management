package com.retail.inventory.inventory_service.application;

import com.retail.inventory.inventory_service.api.dto.request.InventoryRequestDto;
import com.retail.inventory.inventory_service.application.service.InventoryService;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.model.InventoryItem;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.InventoryRepository;
import com.retail.inventory.inventory_service.domain.repository.ProductRepository;
import com.retail.inventory.inventory_service.infrastructure.messaging.InventoryEventProducer;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock
    private InventoryRepository invRepo;
    @Mock
    private ProductRepository productRepo;
    @Mock
    private InventoryEventProducer eventProducer;

    @InjectMocks
    private InventoryService service;

    @Test
    void testAddInventoryItem() {
        // create example inventory item and set each individual property
        LocalDateTime currentTime = LocalDateTime.now();
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        InventoryItem inv = new InventoryItem();
        inv.setId(1L);
        inv.setProduct(product);
        inv.setQuantity(5);
        inv.setLocation("aisle 1");
        inv.setLastUpdated(currentTime);

        // mock repo save()
        when(invRepo.save(any(InventoryItem.class))).thenReturn(inv);

        // mock product repo find(), which is called by service when adding inventory item
        when(productRepo.findById(product.getId())).thenReturn(Optional.of(product));

        // verify correct inventory item was added
        InventoryItem savedInv = service.addInventoryItem(new InventoryRequestDto(1L, product.getId(), 5, "aisle 1", currentTime));

        assertThat(savedInv.getId()).isEqualTo(1L);
        assertThat(savedInv.getProduct()).isEqualTo(product);
        assertThat(savedInv.getQuantity()).isEqualTo(5);
        assertThat(savedInv.getLocation()).isEqualTo("aisle 1");
        assertThat(savedInv.getLastUpdated()).isEqualTo(currentTime);

        // verify repo was called only once
        verify(invRepo, times(1)).save(any(InventoryItem.class));
        verify(productRepo, times(1)).findById(product.getId());
        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void testFindByProduct_Success() {
        // create example inventory item
        LocalDateTime currentTime = LocalDateTime.now();
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00010001", "example 2", "example desc", 19.99, cat);
        InventoryItem inv = new InventoryItem(2L, product, 3, "aisle 2", currentTime);

        // mock repo find()
        when(invRepo.findByProductId(product.getId())).thenReturn(Optional.of(inv));

        // verify inventory item was found
        InventoryItem foundInv = service.getInventoryItem(product.getId());
        assertThat(foundInv.getProduct()).isEqualTo(product);

        // verify repo was called only once
        verify(invRepo, times(1)).findByProductId(product.getId());
    }

    @Test
    void testFindByProduct_Fail() {
        // mock a failed get request
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00020002", "example 3", "example desc", .99, cat);
        when(invRepo.findByProductId(product.getId())).thenReturn(Optional.empty());

        // verify exception was thrown when calling controller
        assertThatThrownBy(() -> service.getInventoryItem(product.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Inventory item not found");
    }

    @Test
    void testGetAllInventory() {
        // populate a list with mock inventory items
        LocalDateTime currentTime = LocalDateTime.now();
        Category cat1 = new Category(1L, "example", "example desc");
        Product product1 = new Product(1L, "00589837", "example", "example desc", 9.99, cat1);
        InventoryItem inv1 = new InventoryItem(1L, product1, 5, "aisle 1", currentTime);

        Category cat2 = new Category(2L, "example 2", "example desc");
        Product product2 = new Product(2L, "00010001", "example 2", "example desc", 19.99, cat2);
        InventoryItem inv2 = new InventoryItem(2L, product2, 3, "aisle 2", currentTime);

        List<InventoryItem> invItems = Arrays.asList(inv1, inv2);

        // mock repo findAll()
        when(invRepo.findAll()).thenReturn(invItems);

        // verify service is getting all inventory items
        List<InventoryItem> foundInv = service.getAllInventory();
        ListAssert<InventoryItem> listAssert = org.assertj.core.api.Assertions.assertThat(foundInv);
        listAssert.hasSize(2);
        listAssert.extracting(InventoryItem::getProduct).containsExactly(product1, product2);

        // verify repo was called only once
        verify(invRepo, times(1)).findAll();
    }

    @Test
    void testAddStock() {
        LocalDateTime currentTime = LocalDateTime.now();
        Category cat = new Category(1L, "example", "example desc");
        Product product = new Product(1L, "00589837", "example", "example desc", 9.99, cat);
        InventoryItem inv = new InventoryItem(1L, product, 5, "aisle 1", currentTime);

        // mock repo find()
        when(invRepo.findByProductId(product.getId())).thenReturn(Optional.of(inv));

        // mock repo save()
        when(invRepo.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // verify stock has increased by 5 to a total of 10
        InventoryItem newStock = service.addStock(inv.getProduct().getId(), 5);
        assertThat(newStock.getQuantity()).isEqualTo(10);

        // verify repo functions were called only once
        verify(invRepo, times(1)).findByProductId(product.getId());
        verify(invRepo, times(1)).save(any(InventoryItem.class));
        verify(eventProducer, times(1)).publish(any());
    }
}
