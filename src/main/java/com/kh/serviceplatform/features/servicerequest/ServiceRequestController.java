package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.provider.matching.ProviderMatchingService;
import com.kh.serviceplatform.features.provider.matching.dto.RecommendedProviderResponse;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.dto.*;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Service Requests", description = "Endpoints for customer problem posting, provider browsing, offers, smart provider matching, and request fulfillment")
@RestController
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final ProviderMatchingService providerMatchingService;

    // CUSTOMER: Create a service request / problem post
    @PostMapping("/api/v1/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Post a service request / problem", description = "Customer posts a new service request with location and details",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or coordinates"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    public ServiceRequestResponse createRequest(@Valid @RequestBody ServiceRequestCreateRequest request) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.createRequest(customerId, request);
    }

    // CUSTOMER: List my service requests
    @GetMapping("/api/v1/service-requests/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List customer's own service requests", description = "Retrieve paginated list of service requests created by current customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service requests retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<ServiceRequestResponse> getMyRequests(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.getMyRequests(customerId, pageable);
    }

    // PROVIDER / PUBLIC: Browse open service requests
    @GetMapping("/api/v1/service-requests")
    @Operation(summary = "Browse open service requests", description = "Providers browse and filter open customer problem posts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service requests retrieved")
    })
    public Page<ServiceRequestSummaryResponse> browseRequests(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) LocalDate preferredDate,
            @RequestParam(required = false) Boolean urgent,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return serviceRequestService.browseRequests(category, city, district, status, minBudget, maxBudget, preferredDate, urgent, search, pageable);
    }

    // PROVIDER: Search nearby service requests by radius
    @GetMapping("/api/v1/service-requests/nearby")
    @Operation(summary = "Search nearby service requests", description = "Providers search open requests near specified coordinates within radius in km")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nearby service requests retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius")
    })
    public Page<ServiceRequestSummaryResponse> searchNearbyRequests(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "10.0") Double radiusKm,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) Boolean urgent,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return serviceRequestService.searchNearbyRequests(latitude, longitude, radiusKm, category, city, district, minBudget, maxBudget, urgent, search, pageable);
    }

    // SMART MATCHING: Recommended Providers for a service request
    @GetMapping("/api/v1/service-requests/{requestId}/recommended-providers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recommended providers for service request", description = "Smart provider matching based on category, location, availability, and rating",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommended providers list retrieved"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public List<RecommendedProviderResponse> getRecommendedProviders(@PathVariable UUID requestId) {
        return providerMatchingService.getRecommendedProviders(requestId);
    }

    // GET service request by ID (public access for visitors/users without account)
    @GetMapping("/api/v1/service-requests/{requestId}")
    @Operation(summary = "Get service request details", description = "Retrieve full details if owner/assigned, or public summary details if browsing without an account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service request details retrieved"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestResponse getRequestById(@PathVariable UUID requestId) {
        UUID currentUserId = SecurityUtils.getOptionalCurrentUserId();
        UserRole currentRole = SecurityUtils.getOptionalCurrentUserRole();
        return serviceRequestService.getRequestById(currentUserId, currentRole, requestId);
    }

    // CUSTOMER: Update service request
    @PutMapping("/api/v1/service-requests/{requestId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update service request", description = "Customer updates their own OPEN service request",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service request updated successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot update non-OPEN request or invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the request owner"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestResponse updateRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ServiceRequestUpdateRequest request
    ) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.updateRequest(customerId, requestId, request);
    }

    // CUSTOMER: Delete service request
    @DeleteMapping("/api/v1/service-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete service request", description = "Customer deletes their own OPEN service request",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Service request deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete non-OPEN request"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the request owner"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public void deleteRequest(@PathVariable UUID requestId) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        serviceRequestService.deleteRequest(customerId, requestId);
    }

    // CUSTOMER: Cancel service request
    @PostMapping("/api/v1/service-requests/{requestId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel service request", description = "Customer cancels their OPEN or ACCEPTED service request",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service request cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel request in current status"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the request owner"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestResponse cancelRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) CancelServiceRequestRequest request
    ) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.cancelRequest(customerId, requestId, request);
    }

    // PROVIDER: Create offer on service request
    @PostMapping("/api/v1/service-requests/{requestId}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Submit an offer for a service request", description = "Provider submits a price proposal and message for an OPEN customer problem",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offer submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Request not OPEN, duplicate offer, or own request"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestOfferResponse createOffer(
            @PathVariable UUID requestId,
            @Valid @RequestBody ServiceRequestOfferRequest request
    ) {
        UUID providerUserId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.createOffer(providerUserId, requestId, request);
    }

    // CUSTOMER / PROVIDER: Get offers for a service request
    @GetMapping("/api/v1/service-requests/{requestId}/offers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get offers for a service request", description = "Customer owner sees all offers; provider sees their own offer",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public Page<ServiceRequestOfferResponse> getRequestOffers(
            @PathVariable UUID requestId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();
        return serviceRequestService.getRequestOffers(currentUserId, currentRole, requestId, pageable);
    }

    // PROVIDER: Get my submitted offers
    @GetMapping("/api/v1/providers/me/offers")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "List provider's own offers", description = "Retrieve paginated list of all offers submitted by current provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offers retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public Page<ServiceRequestOfferResponse> getMyOffers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID providerUserId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.getMyOffers(providerUserId, pageable);
    }

    // PROVIDER: Withdraw own offer
    @PutMapping("/api/v1/offers/{offerId}/withdraw")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Withdraw provider offer", description = "Provider withdraws their pending offer from a service request",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer withdrawn successfully"),
            @ApiResponse(responseCode = "400", description = "Offer is not PENDING"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your offer"),
            @ApiResponse(responseCode = "404", description = "Offer not found")
    })
    public ServiceRequestOfferResponse withdrawOffer(@PathVariable UUID offerId) {
        UUID providerUserId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.withdrawOffer(providerUserId, offerId);
    }

    // CUSTOMER: Accept provider offer
    @PostMapping("/api/v1/service-requests/{requestId}/offers/{offerId}/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Accept provider offer", description = "Customer accepts a provider's offer, locking in the provider and rejecting others",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer accepted and provider assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid offer or request state"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your service request"),
            @ApiResponse(responseCode = "404", description = "Service request or offer not found")
    })
    public ServiceRequestResponse acceptOffer(
            @PathVariable UUID requestId,
            @PathVariable UUID offerId
    ) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.acceptOffer(customerId, requestId, offerId);
    }

    // CUSTOMER: Reject provider offer
    @PostMapping("/api/v1/service-requests/{requestId}/offers/{offerId}/reject")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Reject provider offer", description = "Customer rejects a specific provider's offer",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid offer state"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not your service request"),
            @ApiResponse(responseCode = "404", description = "Service request or offer not found")
    })
    public ServiceRequestOfferResponse rejectOffer(
            @PathVariable UUID requestId,
            @PathVariable UUID offerId
    ) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.rejectOffer(customerId, requestId, offerId);
    }

    // PROVIDER: Start service request
    @PostMapping("/api/v1/service-requests/{requestId}/start")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Start service request", description = "Selected provider marks ACCEPTED request as IN_PROGRESS",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service request marked IN_PROGRESS"),
            @ApiResponse(responseCode = "400", description = "Request not ACCEPTED"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not selected provider"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestResponse startRequest(@PathVariable UUID requestId) {
        UUID providerUserId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.startRequest(providerUserId, requestId);
    }

    // PROVIDER: Complete service request
    @PostMapping("/api/v1/service-requests/{requestId}/complete")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Complete service request", description = "Selected provider marks IN_PROGRESS request as COMPLETED",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service request marked COMPLETED"),
            @ApiResponse(responseCode = "400", description = "Request not IN_PROGRESS"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not selected provider"),
            @ApiResponse(responseCode = "404", description = "Service request not found")
    })
    public ServiceRequestResponse completeRequest(@PathVariable UUID requestId) {
        UUID providerUserId = SecurityUtils.getCurrentUserId();
        return serviceRequestService.completeRequest(providerUserId, requestId);
    }
}
