package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.InventoryService;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.InventorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
    @Mock
    private RestTemplateBuilder builder;
    @Mock
    private RestTemplate rest;
    @Mock
    private InventorySnapshotRepository snapshotRepo;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setup() {
        when(builder.build()).thenReturn(rest);
        inventoryService = new InventoryService(builder, snapshotRepo);
    }

    @Test
    void testGetInventory_usesCachedSnapshot() {
        String sku = "00589837";
        InventorySnapshot cached = new InventorySnapshot(
                sku,
                5,
                "aisle 1",
                LocalDateTime.now(),
                1L
        );

        when(snapshotRepo.findByProductSku(sku)).thenReturn(Optional.of(cached));

        InventorySnapshot result = inventoryService.getInventoryByProductSku(sku);

        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("00589837");
        assertThat(result.getVersion()).isEqualTo(1L);

        verify(snapshotRepo, times(1)).findByProductSku(sku);
        verifyNoInteractions(rest);
    }

    @Test
    void testGetInventory_restFallback() {
        String sku = "00589837";
        InventorySnapshot fresh = new InventorySnapshot(
                sku,
                5,
                "aisle 1",
                LocalDateTime.now(),
                10L
        );

        when(snapshotRepo.findByProductSku(sku)).thenReturn(Optional.empty());
        when(snapshotRepo.save(any(InventorySnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rest.getForObject(anyString(), eq(InventorySnapshot.class))).thenReturn(fresh);

        InventorySnapshot result = inventoryService.getInventoryByProductSku(sku);

        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("00589837");
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo, times(1)).findByProductSku(sku);
        verify(rest, times(1)).getForObject(anyString(), eq(InventorySnapshot.class));
        verify(snapshotRepo).save(fresh);
    }

    @Test
    void testGetFallback_returnCacheOrServiceFailure() {
        String sku = "00589837";

        when(snapshotRepo.findByProductSku(sku)).thenReturn(Optional.empty());

        // fallback called due to missing cache
        InventorySnapshot cacheFailure = inventoryService.getFallback(sku, new RuntimeException("Timeout"));

        assertThat(cacheFailure.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        // fallback called due to throwing exception
        when(snapshotRepo.findByProductSku(sku)).thenThrow(new RuntimeException("DB error"));

        InventorySnapshot serviceFailure = inventoryService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(serviceFailure.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());
    }
}
