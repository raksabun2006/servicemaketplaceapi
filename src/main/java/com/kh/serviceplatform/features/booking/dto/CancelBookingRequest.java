package com.kh.serviceplatform.features.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for cancelling a booking")
public record CancelBookingRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "Reason for cancellation", example = "Schedule conflict; need to reschedule.")
        String reason
) {
}
