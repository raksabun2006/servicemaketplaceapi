package com.kh.serviceplatform.features.auth.dto;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Pattern(regexp = "^$|^[0-9+\\- ]{8,20}$", message = "Invalid phone number format")
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

        Double latitude,

        Double longitude,

        UUID identityDocumentFileId,

        UUID profilePhotoFileId
) {
    public RegisterRequest(
            String fullName,
            String email,
            String phone,
            UserRole role,
            String password,
            String businessName,
            String bio,
            Integer experienceYears,
            String serviceArea,
            String address,
            String city,
            String district,
            Double latitude,
            Double longitude,
            UUID identityDocumentFileId
    ) {
        this(fullName, email, phone, role, password, businessName, bio, experienceYears, serviceArea, address, city, district, latitude, longitude, identityDocumentFileId, null);
    }

    public String getFullName() { return fullName(); }
    public String getEmail() { return email(); }
    public String getPhone() { return phone(); }
    public UserRole getRole() { return role(); }
    public String getPassword() { return password(); }
    public String getBusinessName() { return businessName(); }
    public String getBio() { return bio(); }
    public Integer getExperienceYears() { return experienceYears(); }
    public String getServiceArea() { return serviceArea(); }
    public String getAddress() { return address(); }
    public String getCity() { return city(); }
    public String getDistrict() { return district(); }
    public Double getLatitude() { return latitude(); }
    public Double getLongitude() { return longitude(); }
    public UUID getIdentityDocumentFileId() { return identityDocumentFileId(); }
    public UUID getProfilePhotoFileId() { return profilePhotoFileId(); }
}