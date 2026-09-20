package com.kh.serviceplatform.features.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for submitting a provider response/offer to a service request")
public record ServiceRequestOfferRequest(
        @NotNull(message = "Proposed price is required")
        @DecimalMin(value = "0.01", message = "Proposed price must be at least 0.01")
        @Schema(description = "Proposed service price", example = "35.00")
        BigDecimal proposedPrice,

        @NotBlank(message = "Message is required")
        @Size(max = 3000, message = "Message must not exceed 3000 characters")
        @Schema(description = "Message or proposal details for the customer", example = "I can inspect and repair your air conditioner tomorrow morning.")
        String message,

        @Size(max = 100, message = "Estimated completion time must not exceed 100 characters")
        @Schema(description = "Estimated time to complete work", example = "2 hours")
        String estimatedCompletionTime
) {
}
