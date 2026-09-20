package com.kh.serviceplatform.features.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Public nearby provider summary response payload")
public record NearbyProviderResponse(
        UUID id,
        UUID userId,
        String fullName,
        String avatarUrl,
        String businessName,
        String bio,
        Integer experienceYears,
        String serviceArea,
        String city,
        String district,
        BigDecimal hourlyRate,
        Double averageRating,
        Integer totalReviews,
        boolean isVerified,
        Double distanceKm
) {
}
