package com.kh.serviceplatform.features.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to submit verification documents for provider profile")
public record ProviderVerificationRequest(
        @NotNull(message = "Identity document file ID is required")
        @Schema(description = "UUID of uploaded identity document file", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID identityDocumentFileId,

        @NotNull(message = "Profile photo file ID is required")
        @Schema(description = "UUID of uploaded profile photo file", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID profilePhotoFileId,

        @Schema(description = "Optional additional notes for the verification request", example = "Licensed HVAC technician with 5 years experience")
        String notes
) {
}
