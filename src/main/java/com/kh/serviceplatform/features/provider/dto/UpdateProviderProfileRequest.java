package com.kh.serviceplatform.features.provider.dto;

import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request payload for updating provider profile")
public record UpdateProviderProfileRequest(
        @Size(max = 150)
        @Schema(description = "Full name of the provider user", example = "John Doe")
        String fullName,

        @Size(max = 20)
        @Schema(description = "Contact phone number", example = "+85512345678")
        String phone,

        @Size(max = 500)
        @Schema(description = "Avatar URL path", example = "/api/v1/files/123e4567-e89b-12d3-a456-426614174000")
        String avatarUrl,

        @Size(max = 150)
        @Schema(description = "Business or trading name", example = "Pro Cleaners Co.")
        String businessName,

        @Size(max = 3000)
        @Schema(description = "Provider biography / summary", example = "Professional home cleaning services since 2018.")
        String bio,

        @Min(0)
        @Schema(description = "Years of professional experience", example = "5")
        Integer experienceYears,

        @Size(max = 150)
        @Schema(description = "Operational service area / zone", example = "Phnom Penh")
        String serviceArea,

        @Size(max = 255)
        @Schema(description = "Physical address", example = "123 St 456, Boeung Keng Kang 1")
        String address,

        @Size(max = 100)
        @Schema(description = "City", example = "Phnom Penh")
        String city,

        @Size(max = 100)
        @Schema(description = "District", example = "Chamkar Mon")
        String district,

        @DecimalMin(value = "0.00")
        @Schema(description = "Base hourly rate", example = "25.00")
        BigDecimal hourlyRate,

        @Schema(description = "Latitude coordinate", example = "11.5564")
        Double latitude,

        @Schema(description = "Longitude coordinate", example = "104.9282")
        Double longitude,

        @DecimalMin(value = "0.1")
        @Schema(description = "Service coverage radius in kilometers", example = "10.0")
        Double serviceRadiusKm,

        @Schema(description = "Availability status: AVAILABLE, BUSY, OFFLINE", example = "AVAILABLE")
        AvailabilityStatus availabilityStatus,

        @Size(max = 255)
        @Schema(description = "Working days e.g. MONDAY,TUESDAY,WEDNESDAY", example = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
        String workingDays,

        @Size(max = 10)
        @Schema(description = "Working hours start (HH:mm)", example = "08:00")
        String workingHoursStart,

        @Size(max = 10)
        @Schema(description = "Working hours end (HH:mm)", example = "18:00")
        String workingHoursEnd,

        @Schema(description = "Identity document file ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID identityDocumentFileId,

        @Schema(description = "Profile photo file ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID profilePhotoFileId,

        @Schema(description = "Availability status for accepting bookings", example = "true")
        Boolean isAvailable
) {
}
