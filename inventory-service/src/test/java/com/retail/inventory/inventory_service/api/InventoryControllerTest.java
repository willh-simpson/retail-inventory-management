package com.retail.inventory.inventory_service.api;

import com.retail.inventory.inventory_service.api.controller.InventoryController;
import com.retail.inventory.inventory_service.domain.model.Inventory;
import com.retail.inventory.inventory_service.domain.model.Product;
import com.retail.inventory.inventory_service.domain.repository.InventoryRepository;
import org.assertj.core.api.ListAssert;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {
    @Mock
    private InventoryRepository repo;

    @InjectMocks
    private InventoryController controller;

    @Test
    void testAddInventory() {
        // create example inventory info and set each individual property
        Product product = new Product("00589837", "example", 9.99);
        Inventory inv = new Inventory();
        inv.setId(1L);
        inv.setProduct(product);
        inv.setQuantityOnHand(5);
        inv.setQuantityReserved(3);

        // mock repo save()
        when(repo.save(any(Inventory.class))).thenReturn(inv);

        // verify correct inventory was added
        Inventory savedInv = controller.addInventory(new Inventory(product, 5, 3));
        assertThat(savedInv.getId()).isEqualTo(1L);
        assertThat(savedInv.getProduct()).isEqualTo(product);
        assertThat(savedInv.getQuantityOnHand()).isEqualTo(5);
        assertThat(savedInv.getQuantityReserved()).isEqualTo(3);

        // verify repo was called only once
        verify(repo, times(1)).save(any(Inventory.class));
    }

    @Test
    void testFindByProduct_Success() {
        // create example inventory
        Product product = new Product("00010001", "example 2", 19.99);
        Inventory inv = new Inventory(product, 3, 2);
        inv.setId(2L);

        // mock repo find()
        when(repo.findByProduct(product)).thenReturn(Optional.of(inv));

        // verify inventory was found
        Inventory foundInv = controller.getInventory(product);
        assertThat(foundInv.getProduct()).isEqualTo(product);

        // verify repo was called only once
        verify(repo, times(1)).findByProduct(product);
    }

    @Test
    void testFindByProduct_Fail() {
        // mock a failed get request
        Product product = new Product("00020002", "example 3", .99);
        when(repo.findByProduct(product)).thenReturn(Optional.empty());

        // verify exception was thrown when calling controller
        assertThatThrownBy(() -> controller.getInventory(product))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Inventory item not found");
    }

    @Test
    void testGetAllInventory() {
        // populate a list with mock inventory items
        Product product1 = new Product("00589837", "example", 9.99);
        product1.setId(1L);
        Inventory inv1 = new Inventory(product1, 5, 3);
        inv1.setId(1L);

        Product product2 = new Product("00010001", "example 2", 19.99);
        product2.setId(2L);
        Inventory inv2 = new Inventory(product2, 3, 2);
        inv2.setId(2L);

        List<Inventory> invItems = Arrays.asList(inv1, inv2);

        // mock repo findAll()
        when(repo.findAll()).thenReturn(invItems);

        // verify controller is getting all inventory items
        List<Inventory> foundInv = controller.getAllInventory();
        ListAssert<Inventory> listAssert = org.assertj.core.api.Assertions.assertThat(foundInv);
        listAssert.hasSize(2);
        listAssert.extracting(Inventory::getProduct).containsExactly(product1, product2);

        // verify repo was called only once
        verify(repo, times(1)).findAll();
    }
}
