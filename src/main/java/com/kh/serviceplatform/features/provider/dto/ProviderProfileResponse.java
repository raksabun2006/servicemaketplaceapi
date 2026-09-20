package com.kh.serviceplatform.features.provider.dto;

import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Provider profile response payload")
public record ProviderProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        String businessName,
        String bio,
        Integer experienceYears,
        String serviceArea,
        String address,
        String city,
        String district,
        BigDecimal hourlyRate,
        Double latitude,
        Double longitude,
        Double serviceRadiusKm,
        AvailabilityStatus availabilityStatus,
        String workingDays,
        String workingHoursStart,
        String workingHoursEnd,
        UUID identityDocumentFileId,
        UUID profilePhotoFileId,
        ProviderVerificationStatus verificationStatus,
        String rejectionReason,
        boolean isAvailable,
        boolean isVerified,
        Double averageRating,
        Integer totalReviews,
        Integer completedServices,
        Instant createdAt,
        Instant updatedAt
) {
}
