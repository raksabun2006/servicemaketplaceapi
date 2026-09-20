package com.kh.serviceplatform.features.auth.dto;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Invalid phone number format")
        String phone,

        UserRole role,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @Size(max = 150, message = "Business name cannot exceed 150 characters")
        String businessName,

        @Size(max = 2000, message = "Bio cannot exceed 2000 characters")
        String bio,

        @Min(value = 0, message = "Experience years cannot be negative")
        @Max(value = 100, message = "Experience years cannot exceed 100")
        Integer experienceYears,

        @Size(max = 255, message = "Service area cannot exceed 255 characters")
        String serviceArea,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "District cannot exceed 100 characters")
        String district,

        BigDecimal latitude,

        BigDecimal longitude,

        UUID identityDocumentFileId,

        UUID profilePhotoFileId
) {
}
