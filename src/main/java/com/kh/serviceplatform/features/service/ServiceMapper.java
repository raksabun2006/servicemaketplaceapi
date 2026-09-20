package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    public ServiceResponse toResponse(ServiceOffer service) {
        if (service == null) {
            return null;
        }

        ProviderProfile provider = service.getProvider();
        String businessName = provider != null ? provider.getBusinessName() : null;
        String fullName = provider != null && provider.getUser() != null ? provider.getUser().getFullName() : null;

        String imageUrl = service.getImageFile() != null ? "/api/v1/files/" + service.getImageFile().getId() : null;

        return new ServiceResponse(
                service.getId(),
                provider != null ? provider.getId() : null,
                businessName,
                fullName,
                service.getName(),
                service.getDescription(),
                service.getCategory(),
                service.getPrice(),
                service.getDurationMinutes(),
                imageUrl,
                service.getImageFile() != null ? service.getImageFile().getId() : null,
                service.isAvailable(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
