package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.InventoryService;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.InventorySnapshotEntity;
import com.retail.inventory.order_service.domain.repository.InventorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    void testGetInventoryByProductSku_success() {
        InventorySnapshotEntity snapshot = new InventorySnapshotEntity(
                "00589837",
                5,
                "aisle 1",
                LocalDateTime.now(),
                1L
        );

        when(rest.getForObject("http://inventory-service/api/inventory/" + snapshot.getProductSku(), InventorySnapshotEntity.class)).thenReturn(snapshot);

        InventorySnapshotEntity result = inventoryService.getInventoryByProductSku(snapshot.getProductSku());

        // verify inventory item was successfully found
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo("00589837");
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getVersion()).isEqualTo(1L);

        verify(rest, times(1)).getForObject(anyString(), eq(InventorySnapshotEntity.class));
    }

    @Test
    void testGetInventoryByProductSku_failure() {
        String sku = "does-not-exist";

        when(rest.getForObject("http://inventory-service/api/inventory/" + sku, InventorySnapshotEntity.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // verify exception was handled
        assertThatThrownBy(() -> inventoryService.getInventoryByProductSku(sku))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void testGetFallback_returnCachedSnapshot() {
        String sku = "00589837";
        InventorySnapshotEntity cached = new InventorySnapshotEntity(
                sku,
                5,
                "aisle 1",
                LocalDateTime.now(),
                10L
        );

        // mock previously cached InventorySnapshotEntity already existing locally
        when(snapshotRepo.findByProductSku(sku)).thenReturn(Optional.of(cached));

        InventorySnapshotEntity result = inventoryService.getFallback(sku, new RuntimeException("Service down"));

        // verify fallback was handled properly
        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo(sku);
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo).findByProductSku(sku);
    }

    @Test
    void testGetFallback_returnLocalCacheFailure() {
        String sku = "00589837";

        when(snapshotRepo.findByProductSku(sku)).thenReturn(Optional.empty());

        InventorySnapshotEntity result = inventoryService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo(sku);
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        verify(snapshotRepo).findByProductSku(sku);
    }

    @Test
    void testGetFallback_returnServiceFailure() {
        String sku = "00589837";

        when(snapshotRepo.findByProductSku(sku)).thenThrow(new RuntimeException("Service down"));

        InventorySnapshotEntity result = inventoryService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getProductSku()).isEqualTo(sku);
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());

        verify(snapshotRepo).findByProductSku(sku);
    }
}
