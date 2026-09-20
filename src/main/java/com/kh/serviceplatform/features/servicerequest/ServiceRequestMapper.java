package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.servicerequest.dto.ServiceRequestOfferResponse;
import com.kh.serviceplatform.features.servicerequest.dto.ServiceRequestResponse;
import com.kh.serviceplatform.features.servicerequest.dto.ServiceRequestSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ServiceRequestMapper {

    public ServiceRequestResponse toResponse(
            ServiceRequest request,
            UUID currentUserId,
            UserRole currentRole,
            Double distanceKm
    ) {
        if (request == null) {
            return null;
        }

        User customer = request.getCustomer();
        ProviderProfile provider = request.getSelectedProvider();

        boolean isOwner = customer != null && currentUserId != null && customer.getId().equals(currentUserId);
        boolean isSelectedProvider = provider != null && provider.getUser() != null &&
                currentUserId != null && provider.getUser().getId().equals(currentUserId);
        boolean isAdmin = currentRole == UserRole.ADMIN;

        boolean canViewPrivateDetails = isOwner || isSelectedProvider || isAdmin;

        String customerPhone = canViewPrivateDetails && customer != null ? customer.getPhone() : null;
        String customerEmail = canViewPrivateDetails && customer != null ? customer.getEmail() : null;
        String address = canViewPrivateDetails ? request.getAddress() : null;

        List<String> imageUrls = request.getImages() != null
                ? request.getImages().stream().map(f -> "/api/v1/files/" + f.getId()).toList()
                : Collections.emptyList();

        int offerCount = request.getOffers() != null ? request.getOffers().size() : 0;

        return new ServiceRequestResponse(
                request.getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                customerPhone,
                customerEmail,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getBudgetMin(),
                request.getBudgetMax(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                address,
                request.getCity(),
                request.getDistrict(),
                request.getLatitude(),
                request.getLongitude(),
                distanceKm,
                request.isUrgent(),
                request.getStatus(),
                imageUrls,
                offerCount,
                request.getCancellationReason(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    public ServiceRequestSummaryResponse toSummaryResponse(ServiceRequest request, Double distanceKm) {
        if (request == null) {
            return null;
        }

        int offerCount = request.getOffers() != null ? request.getOffers().size() : 0;

        return new ServiceRequestSummaryResponse(
                request.getId(),
                request.getTitle(),
                request.getCategory(),
                request.getCity(),
                request.getDistrict(),
                distanceKm,
                request.getBudgetMin(),
                request.getBudgetMax(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.isUrgent(),
                request.getStatus(),
                offerCount,
                request.getCreatedAt()
        );
    }

    public ServiceRequestOfferResponse toOfferResponse(ServiceRequestOffer offer) {
        if (offer == null) {
            return null;
        }

        ProviderProfile provider = offer.getProvider();
        User providerUser = provider != null ? provider.getUser() : null;

        return new ServiceRequestOfferResponse(
                offer.getId(),
                offer.getServiceRequest() != null ? offer.getServiceRequest().getId() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getBusinessName() : null,
                providerUser != null ? providerUser.getFullName() : null,
                providerUser != null ? providerUser.getPhone() : null,
                provider != null ? provider.getAverageRating() : null,
                provider != null ? provider.getTotalReviews() : null,
                offer.getProposedPrice(),
                offer.getMessage(),
                offer.getEstimatedCompletionTime(),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
