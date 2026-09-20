package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.provider.dto.NearbyProviderResponse;
import com.kh.serviceplatform.features.provider.dto.ProviderProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class ProviderProfileMapper {

    public ProviderProfileResponse toResponse(ProviderProfile profile) {
        if (profile == null) {
            return null;
        }

        User user = profile.getUser();

        return new ProviderProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getAvatarUrl() : null,
                profile.getBusinessName(),
                profile.getBio(),
                profile.getExperienceYears(),
                profile.getServiceArea(),
                profile.getAddress(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getHourlyRate(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getServiceRadiusKm(),
                profile.getAvailabilityStatus(),
                profile.getWorkingDays(),
                profile.getWorkingHoursStart(),
                profile.getWorkingHoursEnd(),
                profile.getIdentityDocumentFile() != null ? profile.getIdentityDocumentFile().getId() : null,
                profile.getProfilePhotoFile() != null ? profile.getProfilePhotoFile().getId() : null,
                profile.getVerificationStatus(),
                profile.getRejectionReason(),
                profile.isAvailable(),
                profile.isVerified(),
                profile.getAverageRating(),
                profile.getTotalReviews(),
                profile.getCompletedServices(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public NearbyProviderResponse toNearbyResponse(ProviderProfile profile, Double distanceKm) {
        if (profile == null) {
            return null;
        }

        User user = profile.getUser();

        return new NearbyProviderResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getAvatarUrl() : null,
                profile.getBusinessName(),
                profile.getBio(),
                profile.getExperienceYears(),
                profile.getServiceArea(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getHourlyRate(),
                profile.getAverageRating(),
                profile.getTotalReviews(),
                profile.isVerified(),
                distanceKm
        );
    }
}
