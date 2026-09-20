package com.kh.serviceplatform.features.favorite;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.favorite.dto.FavoriteProviderResponse;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import org.springframework.stereotype.Component;

@Component
public class FavoriteMapper {

    public FavoriteProviderResponse toResponse(FavoriteProvider favorite) {
        if (favorite == null) {
            return null;
        }

        ProviderProfile provider = favorite.getProvider();
        User user = provider != null ? provider.getUser() : null;

        return new FavoriteProviderResponse(
                favorite.getId(),
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getAvatarUrl() : null,
                provider != null ? provider.getBio() : null,
                provider != null ? provider.getExperienceYears() : null,
                provider != null ? provider.getServiceArea() : null,
                provider != null ? provider.getCity() : null,
                provider != null ? provider.getDistrict() : null,
                provider != null ? provider.getHourlyRate() : null,
                provider != null ? provider.getAverageRating() : null,
                provider != null ? provider.getTotalReviews() : null,
                provider != null ? provider.getCompletedServices() : null,
                provider != null && provider.isAvailable(),
                provider != null && provider.isVerified(),
                favorite.getCreatedAt()
        );
    }
}
