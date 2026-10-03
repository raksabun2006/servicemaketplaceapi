package com.kh.serviceplatform.features.auth.dto;

import com.kh.serviceplatform.features.auth.enums.UserRole;

import java.util.UUID;

public record RegisterRequest(
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
        this(
                fullName,
                email,
                phone,
                role,
                password,
                businessName,
                bio,
                experienceYears,
                serviceArea,
                address,
                city,
                district,
                latitude,
                longitude,
                identityDocumentFileId,
                null
        );
    }

    public String getFullName() {
        return fullName();
    }

    public String getEmail() {
        return email();
    }

    public String getPhone() {
        return phone();
    }

    public UserRole getRole() {
        return role();
    }

    public String getPassword() {
        return password();
    }

    public String getBusinessName() {
        return businessName();
    }

    public String getBio() {
        return bio();
    }

    public Integer getExperienceYears() {
        return experienceYears();
    }

    public String getServiceArea() {
        return serviceArea();
    }

    public String getAddress() {
        return address();
    }

    public String getCity() {
        return city();
    }

    public String getDistrict() {
        return district();
    }

    public Double getLatitude() {
        return latitude();
    }

    public Double getLongitude() {
        return longitude();
    }

    public UUID getIdentityDocumentFileId() {
        return identityDocumentFileId();
    }

    public UUID getProfilePhotoFileId() {
        return profilePhotoFileId();
    }
}