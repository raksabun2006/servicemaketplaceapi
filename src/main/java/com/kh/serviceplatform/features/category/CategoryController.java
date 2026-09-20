package com.kh.serviceplatform.features.category;

import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public Categories", description = "Public endpoints for browsing service categories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get active categories", description = "Retrieve all active service categories ordered by display order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active categories retrieved")
    })
    public List<CategoryResponse> getActiveCategories() {
        return categoryService.getActiveCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve details of a category by its unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category details found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public CategoryResponse getCategoryById(@PathVariable UUID id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get category by code", description = "Retrieve details of a category by its unique code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category details found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public CategoryResponse getCategoryByCode(@PathVariable String code) {
        return categoryService.getCategoryByCode(code);
    }
}
