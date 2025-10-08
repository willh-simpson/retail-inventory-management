package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.CategoryService;
import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshotEntity;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.repository.CategorySnapshotRepository;
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

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    private RestTemplateBuilder builder;
    @Mock
    private RestTemplate rest;
    @Mock
    private CategorySnapshotRepository snapshotRepo;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setup() {
        when(builder.build()).thenReturn(rest);
        categoryService = new CategoryService(builder, snapshotRepo);
    }

    @Test
    void testGetCategoryByName_success() {
        CategorySnapshotEntity snapshot = new CategorySnapshotEntity(
                "test",
                "desc",
                1L
        );

        when(rest.getForObject("http://inventory-service/api/categories/" + snapshot.getName(), CategorySnapshotEntity.class)).thenReturn(snapshot);

        CategorySnapshotEntity result = categoryService.getCategoryByName(snapshot.getName());

        // verify category was successfully found
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getVersion()).isEqualTo(1L);

        verify(rest, times(1)).getForObject(anyString(), eq(CategorySnapshotEntity.class));
    }

    @Test
    void getCategoryByName_failure() {
        String name = "does-not-exist";

        when(rest.getForObject("http://inventory-service/api/categories/" + name, CategorySnapshotEntity.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // verify exception was handled
        assertThatThrownBy(() -> categoryService.getCategoryByName(name))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void testGetFallback_returnCachedSnapshot() {
        String name = "test";
        CategorySnapshotEntity cached = new CategorySnapshotEntity(
                name,
                "desc",
                10L
        );

        // mock previously cached CategorySnapshotEntity already existing locally
        when(snapshotRepo.findByName(name)).thenReturn(Optional.of(cached));

        CategorySnapshotEntity result = categoryService.getFallback(name, new RuntimeException("Service down"));

        // verify fallback was handled properly
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo).findByName(name);
    }

    @Test
    void testGetFallback_returnLocalCacheFailure() {
        String name = "test";

        when(snapshotRepo.findByName(name)).thenReturn(Optional.empty());

        CategorySnapshotEntity result = categoryService.getFallback(name, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        verify(snapshotRepo).findByName(name);
    }

    @Test
    void testGetFallback_returnServiceFailure() {
        String name = "test";

        when(snapshotRepo.findByName(name)).thenThrow(new RuntimeException("Service down"));

        CategorySnapshotEntity result = categoryService.getFallback(name, new RuntimeException("Service down"));

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());

        verify(snapshotRepo).findByName(name);
    }
}
