package com.kh.serviceplatform.features.provider.application.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record ProviderApplicationRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 150, message = "Business name cannot exceed 150 characters")
        String businessName,

        @Size(max = 2000, message = "Bio cannot exceed 2000 characters")
        String bio,

        @NotNull(message = "Experience years is required")
        @Min(value = 0, message = "Experience years cannot be negative")
        @Max(value = 100, message = "Experience years cannot exceed 100")
        Integer experienceYears,

        @NotBlank(message = "Service area is required")
        @Size(max = 255, message = "Service area cannot exceed 255 characters")
        String serviceArea,

        @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Invalid phone number format")
        String phone,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "District cannot exceed 100 characters")
        String district,

        Double latitude,

        Double longitude,

        UUID identityDocumentFileId,

        UUID profilePhotoFileId
) {
}
