package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.ProductService;
import com.retail.inventory.order_service.domain.model.snapshot.ProductSnapshotEntity;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private RestTemplateBuilder builder;
    @Mock
    private RestTemplate rest;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setup() {
        when(builder.build()).thenReturn(rest);
        productService = new ProductService(builder);
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
        assertThat(result.getSku()).isEqualTo(snapshot.getSku());
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
    void testProductFallback() {
        String sku = "00589837";

        ProductSnapshotEntity snapshot = productService.getProductFallback(sku, new RuntimeException("Service down"));

        // verify fallback was handled properly
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getSku()).isEqualTo(sku);
        assertThat(snapshot.getName()).isEqualTo("Unavailable");
        assertThat(snapshot.getDescription()).isEqualTo("Product service is down");
        assertThat(snapshot.getVersion()).isEqualTo(-1L);
    }
}
