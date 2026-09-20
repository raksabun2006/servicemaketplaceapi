package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import org.springframework.stereotype.Component;

@Component
public class ProviderApplicationMapper {

    public ProviderApplicationResponse toResponse(ProviderApplication app) {
        if (app == null) {
            return null;
        }

        User applicant = app.getUser();
        ProviderApplicationResponse.ApplicantDto applicantDto = applicant != null
                ? new ProviderApplicationResponse.ApplicantDto(
                applicant.getId(),
                applicant.getFullName(),
                applicant.getEmail(),
                applicant.getPhone()
        )
                : null;

        User reviewer = app.getReviewedBy();
        ProviderApplicationResponse.ReviewerDto reviewerDto = reviewer != null
                ? new ProviderApplicationResponse.ReviewerDto(
                reviewer.getId(),
                reviewer.getFullName(),
                reviewer.getEmail()
        )
                : null;

        String identityDocUrl = app.getIdentityDocumentFile() != null
                ? "/api/v1/files/" + app.getIdentityDocumentFile().getId()
                : null;

        String profilePhotoUrl = app.getProfilePhotoFile() != null
                ? "/api/v1/files/" + app.getProfilePhotoFile().getId()
                : null;

        return new ProviderApplicationResponse(
                app.getId(),
                applicantDto,
                app.getBusinessName(),
                app.getBio(),
                app.getExperienceYears(),
                app.getServiceArea(),
                app.getPhone(),
                app.getAddress(),
                app.getCity(),
                app.getDistrict(),
                app.getLatitude(),
                app.getLongitude(),
                app.getApplicationStatus(),
                app.getIdentityDocumentFile() != null ? app.getIdentityDocumentFile().getId() : null,
                identityDocUrl,
                app.getProfilePhotoFile() != null ? app.getProfilePhotoFile().getId() : null,
                profilePhotoUrl,
                app.getRejectionReason(),
                reviewerDto,
                app.getReviewedAt(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
