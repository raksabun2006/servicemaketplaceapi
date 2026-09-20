package com.kh.serviceplatform.features.service.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Service offering response payload")
public record ServiceResponse(
        UUID id,
        UUID providerId,
        String providerBusinessName,
        String providerFullName,
        String name,
        String description,
        ServiceCategory category,
        BigDecimal price,
        Integer durationMinutes,
        String imageUrl,
        UUID imageFileId,
        boolean isAvailable,
        Instant createdAt,
        Instant updatedAt
) {
}
