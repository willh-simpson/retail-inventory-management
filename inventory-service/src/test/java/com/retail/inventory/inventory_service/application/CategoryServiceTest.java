package com.retail.inventory.inventory_service.application;

import com.retail.inventory.inventory_service.api.dto.request.CategoryRequestDto;
import com.retail.inventory.inventory_service.application.service.CategoryService;
import com.retail.inventory.inventory_service.domain.model.Category;
import com.retail.inventory.inventory_service.domain.repository.CategoryRepository;
import com.retail.inventory.inventory_service.infrastructure.messaging.CategoryEventProducer;
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
public class CategoryServiceTest {
    @Mock
    private CategoryRepository repo;
    @Mock
    private CategoryEventProducer eventProducer;

    @InjectMocks
    private CategoryService service;

    @Test
    void testAddCategory() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("example");
        cat.setDescription("example desc");

        // mock repo save()
        when(repo.save(any(Category.class))).thenReturn(cat);

        // verify correct category was added
        Category savedCat = service.addCategory(new CategoryRequestDto(1L, "example", "example desc"));
        assertThat(savedCat.getId()).isEqualTo(1L);
        assertThat(savedCat.getName()).isEqualTo("example");
        assertThat(savedCat.getDescription()).isEqualTo("example desc");

        // verify repo was called only once
        verify(repo, times(1)).save(any(Category.class));
        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void testFindByCategory_Success() {
        Category cat = new Category(1L, "example", "example desc");

        // mock repo find()
        when(repo.findByName(cat.getName())).thenReturn(Optional.of(cat));

        // verify category was found
        Category foundCat = service.getCategory(cat.getName());
        assertThat(foundCat.getName()).isEqualTo(cat.getName());

        // verify repo was called only once
        verify(repo, times(1)).findByName(cat.getName());
    }

    @Test
    void testFindByCategory_Fail() {
        // mock a failed get request
        Category cat = new Category(1L, "example", "example desc");
        when(repo.findByName(cat.getName())).thenReturn(Optional.empty());

        // verify exception was thrown when calling controller
        assertThatThrownBy(() -> service.getCategory(cat.getName()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Category not found");
    }

    @Test
    void testGetAllCategories() {
        // list of mock categories
        Category cat1 = new Category(1L, "example", "example desc");
        Category cat2 = new Category(2L, "example 2", "example desc");
        List<Category> categories = Arrays.asList(cat1, cat2);

        // mock repo findAll()
        when(repo.findAll()).thenReturn(categories);

        // verify service is getting all categories
        List<Category> foundCategories = service.getAllCategories();
        ListAssert<Category> listAssert = org.assertj.core.api.Assertions.assertThat(foundCategories);
        listAssert.hasSize(2);
        listAssert.extracting(Category::getName).containsExactly("example", "example 2");

        // verify repo was called only once
        verify(repo, times(1)).findAll();
    }

    @Test
    void testChangeName() {
        Category cat = new Category(1L, "example", "example desc");

        // mock repo find()
        when(repo.findById(cat.getId())).thenReturn(Optional.of(cat));

        // mock repo save()
        when(repo.save(cat)).thenAnswer(i -> i.getArgument(0));

        // verify category name was changed
        Category newCat = service.changeName(cat.getId(), "new name");
        assertThat(newCat.getName()).isEqualTo("new name");

        // verify repo functions were called only once
        verify(repo, times(1)).findById(cat.getId());
        verify(repo, times(1)).save(any(Category.class));
    }
}
