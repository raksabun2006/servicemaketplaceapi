package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.dto.*;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ServiceRequestService {

    // Customer operations
    ServiceRequestResponse createRequest(UUID customerId, ServiceRequestCreateRequest request);

    Page<ServiceRequestResponse> getMyRequests(UUID customerId, Pageable pageable);

    ServiceRequestResponse getRequestById(UUID userId, UserRole role, UUID requestId);

    ServiceRequestResponse updateRequest(UUID customerId, UUID requestId, ServiceRequestUpdateRequest request);

    void deleteRequest(UUID customerId, UUID requestId);

    ServiceRequestResponse cancelRequest(UUID customerId, UUID requestId, CancelServiceRequestRequest request);

    // Provider / Public browsing operations
    Page<ServiceRequestSummaryResponse> browseRequests(
            ServiceCategory category,
            String city,
            String district,
            ServiceRequestStatus status,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            LocalDate preferredDate,
            Boolean urgent,
            String search,
            Pageable pageable
    );

    default Page<ServiceRequestSummaryResponse> browseRequests(
            ServiceCategory category,
            String city,
            String district,
            ServiceRequestStatus status,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            LocalDate preferredDate,
            String search,
            Pageable pageable
    ) {
        return browseRequests(category, city, district, status, minBudget, maxBudget, preferredDate, null, search, pageable);
    }

    Page<ServiceRequestSummaryResponse> searchNearbyRequests(
            Double latitude,
            Double longitude,
            Double radiusKm,
            ServiceCategory category,
            String city,
            String district,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            Boolean urgent,
            String search,
            Pageable pageable
    );

    default Page<ServiceRequestSummaryResponse> searchNearbyRequests(
            Double latitude,
            Double longitude,
            Double radiusKm,
            ServiceCategory category,
            String city,
            String district,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String search,
            Pageable pageable
    ) {
        return searchNearbyRequests(latitude, longitude, radiusKm, category, city, district, minBudget, maxBudget, null, search, pageable);
    }

    // Provider Offer operations
    ServiceRequestOfferResponse createOffer(UUID providerUserId, UUID requestId, ServiceRequestOfferRequest request);

    Page<ServiceRequestOfferResponse> getRequestOffers(UUID userId, UserRole role, UUID requestId, Pageable pageable);

    Page<ServiceRequestOfferResponse> getMyOffers(UUID providerUserId, Pageable pageable);

    ServiceRequestOfferResponse withdrawOffer(UUID providerUserId, UUID offerId);

    // Customer decision operations
    ServiceRequestResponse acceptOffer(UUID customerId, UUID requestId, UUID offerId);

    ServiceRequestOfferResponse rejectOffer(UUID customerId, UUID requestId, UUID offerId);

    // Provider execution workflow
    ServiceRequestResponse startRequest(UUID providerUserId, UUID requestId);

    ServiceRequestResponse completeRequest(UUID providerUserId, UUID requestId);
}
