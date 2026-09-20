package com.kh.serviceplatform.features.provider.application.dto;

import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ProviderApplicationResponse(
        UUID id,
        ApplicantDto applicant,
        String businessName,
        String bio,
        Integer experienceYears,
        String serviceArea,
        String phone,
        String address,
        String city,
        String district,
        Double latitude,
        Double longitude,
        ProviderApplicationStatus applicationStatus,
        UUID identityDocumentFileId,
        String identityDocumentUrl,
        UUID profilePhotoFileId,
        String profilePhotoUrl,
        String rejectionReason,
        ReviewerDto reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public record ApplicantDto(
            UUID id,
            String fullName,
            String email,
            String phone
    ) {}

    public record ReviewerDto(
            UUID id,
            String fullName,
            String email
    ) {}
}
