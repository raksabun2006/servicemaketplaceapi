package com.kh.serviceplatform.features.servicerequest.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Service request summary for browsing and nearby discovery without sensitive customer details")
public record ServiceRequestSummaryResponse(
        @Schema(description = "Service request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Title of the request", example = "Air conditioner repair")
        String title,

        @Schema(description = "Category of service requested")
        ServiceCategory category,

        @Schema(description = "City where service is required", example = "Phnom Penh")
        String city,

        @Schema(description = "District where service is required", example = "Daun Penh")
        String district,

        @Schema(description = "Distance in kilometers if queried via proximity search", example = "2.4")
        Double distanceKm,

        @Schema(description = "Minimum budget in USD", example = "20.00")
        BigDecimal budgetMin,

        @Schema(description = "Maximum budget in USD", example = "50.00")
        BigDecimal budgetMax,

        @Schema(description = "Preferred service date", example = "2026-10-01")
        LocalDate preferredDate,

        @Schema(description = "Preferred service time slot", example = "Morning (9am - 12pm)")
        String preferredTime,

        @Schema(description = "Whether the request is marked as urgent", example = "false")
        boolean urgent,

        @Schema(description = "Current status of the request")
        ServiceRequestStatus status,

        @Schema(description = "Total number of offers received", example = "3")
        int offerCount,

        @Schema(description = "Creation timestamp in UTC")
        Instant createdAt
) {
        public ServiceRequestSummaryResponse(
                UUID id,
                String title,
                ServiceCategory category,
                String city,
                String district,
                Double distanceKm,
                BigDecimal budgetMin,
                BigDecimal budgetMax,
                LocalDate preferredDate,
                String preferredTime,
                ServiceRequestStatus status,
                int offerCount,
                Instant createdAt
        ) {
                this(id, title, category, city, district, distanceKm, budgetMin, budgetMax, preferredDate, preferredTime, false, status, offerCount, createdAt);
        }
}