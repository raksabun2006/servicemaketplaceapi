package com.kh.serviceplatform.features.category.dto;

import com.kh.serviceplatform.features.file.dto.FileResponse;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String code,
        String description,
        FileResponse iconFile,
        Integer displayOrder,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
