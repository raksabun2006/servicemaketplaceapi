package com.kh.serviceplatform.features.category;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import com.kh.serviceplatform.features.category.dto.CreateCategoryRequest;
import com.kh.serviceplatform.features.category.dto.UpdateCategoryRequest;
import com.kh.serviceplatform.features.file.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Home Cleaning")
                .code("HOME_CLEANING")
                .description("Professional home cleaning services")
                .displayOrder(1)
                .isActive(true)
                .build();
    }

    @Test
    void shouldGetActiveCategories() {
        when(categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                null,
                1,
                true,
                null,
                null
        ));

        List<CategoryResponse> results = categoryService.getActiveCategories();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Home Cleaning", results.get(0).name());
    }

    @Test
    void shouldCreateCategorySuccessfully() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Home Cleaning",
                "HOME_CLEANING",
                "Professional cleaning",
                null,
                1,
                true
        );

        when(categoryRepository.existsByNameIgnoreCase("Home Cleaning")).thenReturn(false);
        when(categoryRepository.existsByCodeIgnoreCase("HOME_CLEANING")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                null,
                1,
                true,
                null,
                null
        ));

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Home Cleaning", response.name());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldThrowWhenCreatingCategoryWithDuplicateName() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Home Cleaning",
                null,
                null,
                null,
                null,
                null
        );

        when(categoryRepository.existsByNameIgnoreCase("Home Cleaning")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Deep Cleaning",
                "DEEP_CLEANING",
                "Deep cleaning service",
                null,
                2,
                true
        );

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Deep Cleaning", category.getId())).thenReturn(false);
        when(categoryRepository.existsByCodeIgnoreCaseAndIdNot("DEEP_CLEANING", category.getId())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(
                category.getId(),
                "Deep Cleaning",
                "DEEP_CLEANING",
                "Deep cleaning service",
                null,
                2,
                true,
                null,
                null
        ));

        CategoryResponse response = categoryService.updateCategory(category.getId(), request);

        assertNotNull(response);
        assertEquals("Deep Cleaning", response.name());
    }

    @Test
    void shouldDeleteCategorySuccessfully() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.deleteCategory(category.getId());

        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldToggleCategoryStatus() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                null,
                1,
                false,
                null,
                null
        ));

        CategoryResponse response = categoryService.toggleCategoryStatus(category.getId());

        assertNotNull(response);
        assertFalse(response.isActive());
    }

    @Test
    void shouldThrowWhenCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(id));
    }

    @Test
    void shouldGetCategoriesPage() {
        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(mock(CategoryResponse.class));

        Page<CategoryResponse> result = categoryService.getCategories("cleaning", true, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
