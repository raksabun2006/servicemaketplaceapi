package com.kh.serviceplatform.features.servicerequest.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request payload for updating an existing OPEN service request")
public record ServiceRequestUpdateRequest(
        @Size(max = 200, message = "Title must not exceed 200 characters")
        @Schema(description = "Updated title", example = "Air conditioner is making loud noise")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Updated detailed description")
        String description,

        @Schema(description = "Updated service category")
        ServiceCategory category,

        @DecimalMin(value = "0.00", message = "Minimum budget must be at least 0.00")
        @Schema(description = "Updated minimum budget", example = "25.00")
        BigDecimal budgetMin,

        @DecimalMin(value = "0.00", message = "Maximum budget must be at least 0.00")
        @Schema(description = "Updated maximum budget", example = "60.00")
        BigDecimal budgetMax,

        @Schema(description = "Updated preferred service date")
        LocalDate preferredDate,

        @Schema(description = "Updated preferred time of day", example = "15:00")
        String preferredTime,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        @Schema(description = "Updated physical address")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "Updated city")
        String city,

        @Size(max = 100, message = "District must not exceed 100 characters")
        @Schema(description = "Updated district")
        String district,

        @Schema(description = "Updated latitude coordinate")
        Double latitude,

        @Schema(description = "Updated longitude coordinate")
        Double longitude,

        @Schema(description = "Whether this is an emergency / urgent service request")
        Boolean urgent,

        @Schema(description = "Updated list of uploaded image file IDs")
        List<UUID> imageFileIds
) {
        public ServiceRequestUpdateRequest(
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
                this(title, description, category, budgetMin, budgetMax, preferredDate, preferredTime, address, city, district, latitude, longitude, null, imageFileIds);
        }
}
