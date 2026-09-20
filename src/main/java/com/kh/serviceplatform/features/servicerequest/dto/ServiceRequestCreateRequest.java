package com.kh.serviceplatform.features.servicerequest.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request payload for creating a new service request / problem post")
public record ServiceRequestCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        @Schema(description = "Brief title of the problem/request", example = "Air conditioner is not cooling")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Detailed explanation of the issue", example = "My home air conditioner turns on but does not produce cold air.")
        String description,

        @NotNull(message = "Category is required")
        @Schema(description = "Category of service requested", example = "AC_REPAIR")
        ServiceCategory category,

        @DecimalMin(value = "0.00", message = "Minimum budget must be at least 0.00")
        @Schema(description = "Minimum expected budget", example = "20.00")
        BigDecimal budgetMin,

        @DecimalMin(value = "0.00", message = "Maximum budget must be at least 0.00")
        @Schema(description = "Maximum expected budget", example = "50.00")
        BigDecimal budgetMax,

        @Schema(description = "Preferred service date", example = "2026-09-20")
        LocalDate preferredDate,

        @Schema(description = "Preferred time of day", example = "14:00")
        String preferredTime,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        @Schema(description = "Physical address location", example = "Street 271, Sangkat Boeung Tumpun")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "City", example = "Phnom Penh")
        String city,

        @Size(max = 100, message = "District must not exceed 100 characters")
        @Schema(description = "District", example = "Meanchey")
        String district,

        @Schema(description = "Latitude coordinate", example = "11.5435")
        Double latitude,

        @Schema(description = "Longitude coordinate", example = "104.8997")
        Double longitude,

        @Schema(description = "Whether this is an emergency / urgent service request", example = "false")
        Boolean urgent,

        @Schema(description = "List of uploaded problem photo file IDs")
        List<UUID> imageFileIds
) {
        public ServiceRequestCreateRequest(
                String title,
                String description,
                ServiceCategory category,
                BigDecimal budgetMin,
                BigDecimal budgetMax,
                LocalDate preferredDate,
                String preferredTime,
                String address,
                String city,
                String district,
                Double latitude,
                Double longitude,
                List<UUID> imageFileIds
        ) {
                this(title, description, category, budgetMin, budgetMax, preferredDate, preferredTime, address, city, district, latitude, longitude, false, imageFileIds);
        }
}
