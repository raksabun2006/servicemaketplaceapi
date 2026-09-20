package com.kh.serviceplatform.features.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for rejecting a booking by provider")
public record RejectBookingRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "Reason for rejecting the booking request", example = "Fully booked during requested time slot.")
        String reason
) {
}
