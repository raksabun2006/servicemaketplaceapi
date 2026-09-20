package com.kh.serviceplatform.features.category;

import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import com.kh.serviceplatform.features.file.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final FileMapper fileMapper;

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getIconFile() != null ? fileMapper.toResponse(category.getIconFile()) : null,
                category.getDisplayOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
