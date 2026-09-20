package com.kh.serviceplatform.features.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request payload for creating a service booking")
public record CreateBookingRequest(
        @Schema(description = "UUID of the service offer (if booking a catalog service)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID serviceId,

        @Schema(description = "UUID of the service request (if booking from a customer request)", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID serviceRequestId,

        @Schema(description = "UUID of the accepted offer", example = "123e4567-e89b-12d3-a456-426614174002")
        UUID acceptedOfferId,

        @Schema(description = "UUID of the provider profile (if booking directly)", example = "123e4567-e89b-12d3-a456-426614174003")
        UUID providerId,

        @Schema(description = "Scheduled date and time in UTC ISO format", example = "2026-10-01T10:00:00Z")
        Instant scheduledAt,

        @Schema(description = "Scheduled date", example = "2026-10-01")
        LocalDate scheduledDate,

        @Schema(description = "Scheduled start time (HH:mm)", example = "10:00")
        String scheduledStartTime,

        @Schema(description = "Scheduled end time (HH:mm)", example = "12:00")
        String scheduledEndTime,

        @NotBlank(message = "Service location address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        @Schema(description = "Physical service address", example = "No. 45, Street 240, Daun Penh")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "City", example = "Phnom Penh")
        String city,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        @Schema(description = "Special instructions or notes for the provider", example = "Please call when arrived at the gate.")
        String notes
) {
        public CreateBookingRequest(UUID serviceId, Instant scheduledAt, String address, String city, String notes) {
                this(serviceId, null, null, null, scheduledAt, null, null, null, address, city, notes);
        }
}
