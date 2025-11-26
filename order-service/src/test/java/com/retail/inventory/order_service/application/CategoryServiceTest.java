package com.retail.inventory.order_service.application;

import com.retail.inventory.order_service.application.service.CategoryService;
import com.retail.inventory.order_service.domain.model.snapshot.CategorySnapshot;
import com.retail.inventory.order_service.domain.model.snapshot.ExceptionVersion;
import com.retail.inventory.order_service.domain.repository.snapshot.CategorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
    void testGetCategory_usesCachedSnapshot() {
        String name = "test";
        CategorySnapshot cached = new CategorySnapshot(
                name,
                "test",
                1L
        );

        when(snapshotRepo.findByName(name)).thenReturn(Optional.of(cached));

        CategorySnapshot result = categoryService.getCategoryByName(name);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getVersion()).isEqualTo(1L);

        verify(snapshotRepo, times(1)).findByName(name);
        verifyNoInteractions(rest);
    }

    @Test
    void testGetCategory_restFallback() {
        String name = "test";
        CategorySnapshot fresh = new CategorySnapshot(
                name,
                "test",
                10L
        );

        when(snapshotRepo.findByName(name)).thenReturn(Optional.empty());
        when(snapshotRepo.save(any(CategorySnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rest.getForObject(anyString(), eq(CategorySnapshot.class))).thenReturn(fresh);

        CategorySnapshot result = categoryService.getCategoryByName(name);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getVersion()).isEqualTo(10L);

        verify(snapshotRepo, times(1)).findByName(name);
        verify(rest, times(1)).getForObject(anyString(), eq(CategorySnapshot.class));
        verify(snapshotRepo).save(fresh);
    }

    @Test
    void testGetFallback_returnCacheOrServiceFailure() {
        String name = "test";

        when(snapshotRepo.findByName(name)).thenReturn(Optional.empty());

        // fallback called due to missing cache
        CategorySnapshot cacheFailure = categoryService.getFallback(name, new RuntimeException("Timeout"));

        assertThat(cacheFailure.getVersion()).isEqualTo(ExceptionVersion.LOCAL_CACHE_FAILURE.getVersion());

        // fallback called due to throwing exception
        when(snapshotRepo.findByName(name)).thenThrow(new RuntimeException("DB error"));

        CategorySnapshot serviceFailure = categoryService.getFallback(name, new RuntimeException("Service down"));

        assertThat(serviceFailure.getVersion()).isEqualTo(ExceptionVersion.SERVICE_FAILURE.getVersion());
    }
}
