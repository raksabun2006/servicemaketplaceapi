package com.kh.serviceplatform.features.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Pattern(regexp = "^[A-Z0-9_\\-]+$", message = "Code must only contain uppercase alphanumeric characters, underscores, and hyphens")
        @Size(max = 60, message = "Code must not exceed 60 characters")
        String code,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        UUID iconFileId,

        Integer displayOrder,

        Boolean isActive
) {
}
