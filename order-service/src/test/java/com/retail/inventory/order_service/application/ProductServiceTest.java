package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.ProductService;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshot;
import com.retail.inventory.order_service.domain.repository.snapshot.ProductSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private RestTemplateBuilder builder;
    @Mock
    private RestTemplate rest;
    @Mock
    private ProductSnapshotRepository snapshotRepo;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setup() {
        when(builder.build()).thenReturn(rest);
        productService = new ProductService(builder, snapshotRepo);
    }

    @Test
    void testGetProduct_usesCachedSnapshot() {
        String sku = "00589837";
        ProductSnapshot cached = new ProductSnapshot(
                sku,
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );

        when(snapshotRepo.findBySku(sku)).thenReturn(Optional.of(cached));

        ProductSnapshot result = productService.getProductBySku(sku);

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("00589837");
        assertThat(result.getVersion()).isEqualTo(1L);

        verify(snapshotRepo, times(1)).findBySku(sku);
        verifyNoInteractions(rest);
    }

    @Test
    void testGetProduct_restFallback() {
        String sku = "00589837";
        ProductSnapshot fresh = new ProductSnapshot(
                sku,
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                10L
        );

        when(snapshotRepo.findBySku(sku)).thenReturn(Optional.empty());
        when(snapshotRepo.save(any(ProductSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rest.getForObject(anyString(), eq(ProductSnapshot.class))).thenReturn(fresh);

        ProductSnapshot result = productService.getProductBySku(sku);

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("00589837");
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo, times(1)).findBySku(sku);
        verify(rest, times(1)).getForObject(anyString(), eq(ProductSnapshot.class));
        verify(snapshotRepo).save(fresh);
    }

    @Test
    void testGetFallback_returnCacheOrServiceFailure() {
        String sku = "00589837";

        when(snapshotRepo.findBySku(sku)).thenReturn(Optional.empty());

        // fallback called due to missing cache
        ProductSnapshot cacheFailure = productService.getFallback(sku, new RuntimeException("Timeout"));

        assertThat(cacheFailure.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        // fallback called due to throwing exception
        when(snapshotRepo.findBySku(sku)).thenThrow(new RuntimeException("DB error"));

        ProductSnapshot serviceFailure = productService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(serviceFailure.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());
    }
}
