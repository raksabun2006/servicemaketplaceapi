package com.kh.serviceplatform.features.provider.dto;

import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating provider availability settings")
public record UpdateAvailabilityRequest(
        @Schema(description = "Availability status: AVAILABLE, BUSY, OFFLINE", example = "AVAILABLE")
        AvailabilityStatus availabilityStatus,

        @Size(max = 255)
        @Schema(description = "Comma-separated working days", example = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
        String workingDays,

        @Size(max = 10)
        @Schema(description = "Daily starting working hour (HH:mm)", example = "08:00")
        String workingHoursStart,

        @Size(max = 10)
        @Schema(description = "Daily ending working hour (HH:mm)", example = "18:00")
        String workingHoursEnd,

        @DecimalMin(value = "0.1", message = "Service radius must be at least 0.1 km")
        @Schema(description = "Service coverage radius in km", example = "10.0")
        Double serviceRadiusKm
) {
}
