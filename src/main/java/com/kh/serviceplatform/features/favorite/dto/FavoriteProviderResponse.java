package com.kh.serviceplatform.features.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Customer favorite provider details")
public record FavoriteProviderResponse(
        UUID favoriteId,
        UUID providerId,
        String businessName,
        String fullName,
        String avatarUrl,
        String bio,
        Integer experienceYears,
        String serviceArea,
        String city,
        String district,
        BigDecimal hourlyRate,
        Double averageRating,
        Integer totalReviews,
        Integer completedServices,
        boolean isAvailable,
        boolean isVerified,
        Instant favoritedAt
) {
}
