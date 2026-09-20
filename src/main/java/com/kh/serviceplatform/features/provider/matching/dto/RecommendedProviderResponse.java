package com.kh.serviceplatform.features.provider.matching.dto;

import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Recommended provider details for smart matching")
public record RecommendedProviderResponse(
        @Schema(description = "Provider ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID providerId,

        @Schema(description = "Business name", example = "Pisey Home Services")
        String businessName,

        @Schema(description = "Provider full name", example = "Pisey Chan")
        String fullName,

        @Schema(description = "Provider avatar URL")
        String avatarUrl,

        @Schema(description = "Average star rating", example = "4.8")
        Double rating,

        @Schema(description = "Total completed services count", example = "127")
        Integer completedServices,

        @Schema(description = "Years of experience", example = "6")
        Integer experienceYears,

        @Schema(description = "Calculated distance in km", example = "2.4")
        Double distanceKm,

        @Schema(description = "Current availability status", example = "AVAILABLE")
        AvailabilityStatus availabilityStatus,

        @Schema(description = "Whether provider is verified", example = "true")
        boolean verified
) {
}
