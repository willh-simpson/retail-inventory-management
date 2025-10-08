package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.ProductService;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
import com.retail.inventory.order_service.domain.repository.ProductSnapshotRepository;
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

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    void testGetProductBySku_success() {
        ProductSnapshotEntity snapshot = new ProductSnapshotEntity(
                "00589837",
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                1L
        );

        when(rest.getForObject("http://inventory-service/api/products/" + snapshot.getSku(), ProductSnapshotEntity.class)).thenReturn(snapshot);

        ProductSnapshotEntity result = productService.getProductBySku(snapshot.getSku());

        // verify product was successfully found
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("00589837");
        assertThat(result.getName()).isEqualTo("test");

        verify(rest, times(1)).getForObject(anyString(), eq(ProductSnapshotEntity.class));
    }

    @Test
    void getProductBySku_failure() {
        String sku = "does-not-exist";

        when(rest.getForObject("http://inventory-service/api/products/" + sku, ProductSnapshotEntity.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // verify exception was handled
        assertThatThrownBy(() -> productService.getProductBySku(sku))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void testGetFallback_returnCachedSnapshot() {
        String sku = "00589837";
        ProductSnapshotEntity cached = new ProductSnapshotEntity(
                sku,
                "test",
                "desc",
                9.99,
                Map.of("category", "test"),
                10L
        );

        // mock previously cached ProductSnapshotEntity already existing locally
        when(snapshotRepo.findBySku(sku)).thenReturn(Optional.of(cached));

        ProductSnapshotEntity result = productService.getFallback(sku, new RuntimeException("Service down"));

        // verify fallback was handled properly
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(sku);
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo).findBySku(sku);
    }

    @Test
    void testGetFallback_returnLocalCacheFailure() {
        String sku = "00589837";

        when(snapshotRepo.findBySku(sku)).thenReturn(Optional.empty());

        ProductSnapshotEntity result = productService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(sku);
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        verify(snapshotRepo).findBySku(sku);
    }

    @Test
    void testGetFallback_returnServiceFailure() {
        String sku = "00589837";

        when(snapshotRepo.findBySku(sku)).thenThrow(new RuntimeException("Service down"));

        ProductSnapshotEntity result = productService.getFallback(sku, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(sku);
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());

        verify(snapshotRepo).findBySku(sku);
    }
}
