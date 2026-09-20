package com.kh.serviceplatform.features.service.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request payload for updating an existing service offering")
public record UpdateServiceRequest(
        @Size(max = 150, message = "Service name must not exceed 150 characters")
        @Schema(description = "Updated name of the service", example = "Deep House Cleaning (Premium)")
        String name,

        @Size(max = 3000, message = "Description must not exceed 3000 characters")
        @Schema(description = "Updated service description", example = "Complete eco-friendly cleaning including kitchen and bathroom.")
        String description,

        @Schema(description = "Updated service category", example = "CLEANING")
        ServiceCategory category,

        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Schema(description = "Updated price", example = "50.00")
        BigDecimal price,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Schema(description = "Updated estimated duration in minutes", example = "150")
        Integer durationMinutes,

        @Schema(description = "Updated image file ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID imageFileId,

        @Schema(description = "Updated availability status", example = "true")
        Boolean isAvailable
) {
}
