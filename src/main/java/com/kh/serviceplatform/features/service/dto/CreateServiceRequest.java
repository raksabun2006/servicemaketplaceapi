package com.kh.serviceplatform.features.service.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request payload for creating a new service offering")
public record CreateServiceRequest(
        @NotBlank(message = "Service name is required")
        @Size(max = 150, message = "Service name must not exceed 150 characters")
        @Schema(description = "Name of the service", example = "Deep House Cleaning")
        String name,

        @Size(max = 3000, message = "Description must not exceed 3000 characters")
        @Schema(description = "Detailed service description", example = "Complete cleaning of 2-bedroom apartment including kitchen and bathroom.")
        String description,

        @NotNull(message = "Category is required")
        @Schema(description = "Service category", example = "CLEANING")
        ServiceCategory category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Schema(description = "Price for the service", example = "45.00")
        BigDecimal price,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Schema(description = "Estimated duration in minutes", example = "120")
        Integer durationMinutes,

        @Schema(description = "Image file ID for service picture", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID imageFileId,

        @Schema(description = "Availability of the service", example = "true")
        Boolean isAvailable
) {
}
