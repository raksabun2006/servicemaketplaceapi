package com.kh.serviceplatform.features.category;

import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import com.kh.serviceplatform.features.category.dto.CreateCategoryRequest;
import com.kh.serviceplatform.features.category.dto.UpdateCategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getActiveCategories();

    Page<CategoryResponse> getCategories(String search, Boolean isActive, Pageable pageable);

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse getCategoryByCode(String code);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);

    CategoryResponse toggleCategoryStatus(UUID id);
}
