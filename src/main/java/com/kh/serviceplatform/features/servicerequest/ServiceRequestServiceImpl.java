package com.kh.serviceplatform.features.servicerequest;

import com.kh.serviceplatform.common.cache.CacheNames;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.matching.ProviderMatchingService;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.dto.*;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "title", "category", "budgetMin", "budgetMax", "preferredDate", "urgent", "createdAt", "updatedAt"
    );

    private final ServiceRequestRepository requestRepository;
    private final ServiceRequestOfferRepository offerRepository;
    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final BookingRepository bookingRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;
    private final ProviderMatchingService providerMatchingService;
    private final ServiceRequestMapper mapper;

    @Override
    @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    public ServiceRequestResponse createRequest(UUID customerId, ServiceRequestCreateRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active");
        }

        if (request.latitude() != null || request.longitude() != null) {
            GeoUtils.validateCoordinates(request.latitude(), request.longitude());
        }

        Set<StoredFile> images = new LinkedHashSet<>();
        if (request.imageFileIds() != null && !request.imageFileIds().isEmpty()) {
            for (UUID fileId : request.imageFileIds()) {
                StoredFile file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new BadRequestException("Image file not found with ID: " + fileId));
                if (!file.getOwner().getId().equals(customerId)) {
                    throw new ForbiddenException("You do not own the uploaded image: " + fileId);
                }
                images.add(file);
            }
        }

        boolean isUrgent = Boolean.TRUE.equals(request.urgent());

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .customer(customer)
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .budgetMin(request.budgetMin())
                .budgetMax(request.budgetMax())
                .preferredDate(request.preferredDate())
                .preferredTime(request.preferredTime() != null ? request.preferredTime().trim() : null)
                .address(request.address().trim())
                .city(request.city() != null ? request.city().trim() : null)
                .district(request.district() != null ? request.district().trim() : null)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .urgent(isUrgent)
                .status(ServiceRequestStatus.OPEN)
                .images(images)
                .build();

        ServiceRequest saved = requestRepository.save(serviceRequest);
        log.info("Created service request ID: {} (urgent={}) by customer: {}", saved.getId(), isUrgent, customerId);

        // Notify matching providers (especially for urgent requests)
        try {
            List<ProviderProfile> matchingProviders = providerMatchingService.findMatchingProvidersForRequest(saved);
            for (ProviderProfile provider : matchingProviders) {
                if (provider.getUser() != null && !provider.getUser().getId().equals(customerId)) {
                    String notifTitle = isUrgent ? "🚨 URGENT: New Service Request Nearby" : "New Service Request Nearby";
                    String notifBody = "A new " + saved.getCategory() + " request was posted in " + (saved.getCity() != null ? saved.getCity() : "your area") + ": " + saved.getTitle();
                    notificationService.sendNotification(
                            provider.getUser(),
                            NotificationType.NEW_REQUEST_NEARBY,
                            notifTitle,
                            notifBody,
                            saved.getId(),
                            "SERVICE_REQUEST"
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify matching providers for request {}: {}", saved.getId(), e.getMessage());
        }

        return mapper.toResponse(saved, customerId, UserRole.CUSTOMER, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> getMyRequests(UUID customerId, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return requestRepository.findByCustomerId(customerId, safePageable)
                .map(r -> mapper.toResponse(r, customerId, UserRole.CUSTOMER, null));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.SERVICE_REQUESTS,
            key = "#requestId",
            condition = "#userId == null"
    )
    public ServiceRequestResponse getRequestById(UUID userId, UserRole role, UUID requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        return mapper.toResponse(request, userId, role, null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public ServiceRequestResponse updateRequest(UUID customerId, UUID requestId, ServiceRequestUpdateRequest update) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateCustomerOwnership(request, customerId);

        if (request.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BadRequestException("Cannot update service request with status: " + request.getStatus() + ". Only OPEN requests can be modified.");
        }

        if (update.title() != null && !update.title().isBlank()) {
            request.setTitle(update.title().trim());
        }
        if (update.description() != null && !update.description().isBlank()) {
            request.setDescription(update.description().trim());
        }
        if (update.category() != null) {
            request.setCategory(update.category());
        }
        if (update.budgetMin() != null) {
            request.setBudgetMin(update.budgetMin());
        }
        if (update.budgetMax() != null) {
            request.setBudgetMax(update.budgetMax());
        }
        if (update.preferredDate() != null) {
            request.setPreferredDate(update.preferredDate());
        }
        if (update.preferredTime() != null) {
            request.setPreferredTime(update.preferredTime().trim());
        }
        if (update.address() != null && !update.address().isBlank()) {
            request.setAddress(update.address().trim());
        }
        if (update.city() != null) {
            request.setCity(update.city().trim());
        }
        if (update.district() != null) {
            request.setDistrict(update.district().trim());
        }
        if (update.urgent() != null) {
            request.setUrgent(update.urgent());
        }
        if (update.latitude() != null || update.longitude() != null) {
            GeoUtils.validateCoordinates(
                    update.latitude() != null ? update.latitude() : request.getLatitude(),
                    update.longitude() != null ? update.longitude() : request.getLongitude()
            );
            request.setLatitude(update.latitude());
            request.setLongitude(update.longitude());
        }

        if (update.imageFileIds() != null) {
            Set<StoredFile> images = new LinkedHashSet<>();
            for (UUID fileId : update.imageFileIds()) {
                StoredFile file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new BadRequestException("Image file not found with ID: " + fileId));
                if (!file.getOwner().getId().equals(customerId)) {
                    throw new ForbiddenException("You do not own the uploaded image: " + fileId);
                }
                images.add(file);
            }
            request.setImages(images);
        }

        ServiceRequest saved = requestRepository.save(request);
        log.info("Updated service request ID: {} by customer: {}", requestId, customerId);
        return mapper.toResponse(saved, customerId, UserRole.CUSTOMER, null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public void deleteRequest(UUID customerId, UUID requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateCustomerOwnership(request, customerId);

        if (request.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BadRequestException("Cannot delete service request with status: " + request.getStatus() + ". Only OPEN requests can be deleted.");
        }

        requestRepository.delete(request);
        log.info("Deleted service request ID: {} by customer: {}", requestId, customerId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public ServiceRequestResponse cancelRequest(UUID customerId, UUID requestId, CancelServiceRequestRequest cancelRequest) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateCustomerOwnership(request, customerId);

        if (request.getStatus() != ServiceRequestStatus.OPEN && request.getStatus() != ServiceRequestStatus.ACCEPTED) {
            throw new BadRequestException("Cannot cancel service request with status: " + request.getStatus());
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        if (cancelRequest != null && cancelRequest.reason() != null) {
            request.setCancellationReason(cancelRequest.reason().trim());
        }

        // Withdraw/reject pending offers
        List<ServiceRequestOffer> offers = offerRepository.findByServiceRequestId(requestId);
        for (ServiceRequestOffer offer : offers) {
            if (offer.getStatus() == ServiceOfferStatus.PENDING) {
                offer.setStatus(ServiceOfferStatus.WITHDRAWN);
            }
        }

        ServiceRequest saved = requestRepository.save(request);
        log.info("Cancelled service request ID: {} by customer: {}", requestId, customerId);
        return mapper.toResponse(saved, customerId, UserRole.CUSTOMER, null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.SERVICE_REQUESTS,
            key = "T(java.lang.String).format('browse:%s:%s:%s:%s:%s:%s:%s:%s:%s:%d:%d:%s', " +
                  "#category, #city, #district, #status, #minBudget, #maxBudget, #preferredDate, #urgent, #search, " +
                  "#pageable.pageNumber, #pageable.pageSize, #pageable.sort)"
    )
    public Page<ServiceRequestSummaryResponse> browseRequests(
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
    ) {
        Pageable safePageable = sanitizePageable(pageable);
        ServiceRequestStatus filterStatus = status != null ? status : ServiceRequestStatus.OPEN;

        Specification<ServiceRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), filterStatus));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (urgent != null) {
                predicates.add(cb.equal(root.get("urgent"), urgent));
            }

            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.trim().toLowerCase() + "%"));
            }

            if (district != null && !district.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + district.trim().toLowerCase() + "%"));
            }

            if (minBudget != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budgetMin"), minBudget));
            }

            if (maxBudget != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budgetMax"), maxBudget));
            }

            if (preferredDate != null) {
                predicates.add(cb.equal(root.get("preferredDate"), preferredDate));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return requestRepository.findAll(spec, safePageable)
                .map(r -> mapper.toSummaryResponse(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestSummaryResponse> searchNearbyRequests(
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
    ) {
        GeoUtils.validateCoordinates(latitude, longitude);
        double maxRadius = radiusKm != null ? radiusKm : 10.0;
        GeoUtils.validateRadius(maxRadius);

        List<ServiceRequest> openRequests = requestRepository.findByStatusAndHasCoordinates(ServiceRequestStatus.OPEN);

        List<ServiceRequestSummaryResponse> nearbyList = new ArrayList<>();

        for (ServiceRequest req : openRequests) {
            if (category != null && req.getCategory() != category) {
                continue;
            }
            if (urgent != null && req.isUrgent() != urgent) {
                continue;
            }
            if (city != null && (req.getCity() == null || !req.getCity().toLowerCase().contains(city.trim().toLowerCase()))) {
                continue;
            }
            if (district != null && (req.getDistrict() == null || !req.getDistrict().toLowerCase().contains(district.trim().toLowerCase()))) {
                continue;
            }
            if (minBudget != null && (req.getBudgetMin() == null || req.getBudgetMin().compareTo(minBudget) < 0)) {
                continue;
            }
            if (maxBudget != null && (req.getBudgetMax() == null || req.getBudgetMax().compareTo(maxBudget) > 0)) {
                continue;
            }
            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                boolean matchTitle = req.getTitle() != null && req.getTitle().toLowerCase().contains(term);
                boolean matchDesc = req.getDescription() != null && req.getDescription().toLowerCase().contains(term);
                if (!matchTitle && !matchDesc) {
                    continue;
                }
            }

            double distance = GeoUtils.calculateDistanceKm(latitude, longitude, req.getLatitude(), req.getLongitude());
            if (distance <= maxRadius) {
                nearbyList.add(mapper.toSummaryResponse(req, distance));
            }
        }

        // Sort by distance ascending
        nearbyList.sort(Comparator.comparing(ServiceRequestSummaryResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));

        int page = pageable != null ? pageable.getPageNumber() : 0;
        int size = pageable != null && pageable.getPageSize() > 0 ? pageable.getPageSize() : 20;

        int fromIndex = Math.min(page * size, nearbyList.size());
        int toIndex = Math.min(fromIndex + size, nearbyList.size());
        List<ServiceRequestSummaryResponse> pageContent = nearbyList.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, pageable != null ? pageable : PageRequest.of(0, 20), nearbyList.size());
    }

    @Override
    public ServiceRequestOfferResponse createOffer(UUID providerUserId, UUID requestId, ServiceRequestOfferRequest request) {
        ProviderProfile provider = providerProfileRepository.findByUserId(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for user: " + providerUserId));

        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        if (serviceRequest.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BadRequestException("Cannot submit offer for service request with status: " + serviceRequest.getStatus() + ". Must be OPEN.");
        }

        if (serviceRequest.getCustomer().getId().equals(providerUserId)) {
            throw new BadRequestException("Providers cannot submit offers on their own service requests");
        }

        if (offerRepository.existsByServiceRequestIdAndProviderIdAndStatus(requestId, provider.getId(), ServiceOfferStatus.PENDING)) {
            throw new BadRequestException("You already have an active pending offer on this service request");
        }

        ServiceRequestOffer offer = ServiceRequestOffer.builder()
                .serviceRequest(serviceRequest)
                .provider(provider)
                .proposedPrice(request.proposedPrice())
                .message(request.message().trim())
                .estimatedCompletionTime(request.estimatedCompletionTime() != null ? request.estimatedCompletionTime().trim() : null)
                .status(ServiceOfferStatus.PENDING)
                .build();

        ServiceRequestOffer saved = offerRepository.save(offer);
        log.info("Created offer ID: {} for service request ID: {} by provider: {}", saved.getId(), requestId, provider.getId());

        // Notify customer
        if (serviceRequest.getCustomer() != null) {
            notificationService.sendNotification(
                    serviceRequest.getCustomer(),
                    NotificationType.NEW_OFFER,
                    "New Offer Received",
                    "Provider " + provider.getBusinessName() + " submitted an offer of $" + request.proposedPrice() + " for '" + serviceRequest.getTitle() + "'",
                    serviceRequest.getId(),
                    "SERVICE_REQUEST"
            );
        }

        return mapper.toOfferResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestOfferResponse> getRequestOffers(UUID userId, UserRole role, UUID requestId, Pageable pageable) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        boolean isCustomerOwner = request.getCustomer().getId().equals(userId);
        boolean isAdmin = role == UserRole.ADMIN;

        Pageable safePageable = sanitizePageable(pageable);

        if (isCustomerOwner || isAdmin) {
            return offerRepository.findByServiceRequestId(requestId, safePageable)
                    .map(mapper::toOfferResponse);
        }

        // Provider: can only see their own offer
        ProviderProfile provider = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("You do not have permission to view offers for this request"));

        Optional<ServiceRequestOffer> myOffer = offerRepository.findByServiceRequestIdAndProviderId(requestId, provider.getId());
        List<ServiceRequestOfferResponse> content = myOffer.map(mapper::toOfferResponse).stream().toList();
        return new PageImpl<>(content, safePageable, content.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestOfferResponse> getMyOffers(UUID providerUserId, Pageable pageable) {
        ProviderProfile provider = providerProfileRepository.findByUserId(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for user: " + providerUserId));

        Pageable safePageable = sanitizePageable(pageable);
        return offerRepository.findByProviderId(provider.getId(), safePageable)
                .map(mapper::toOfferResponse);
    }

    @Override
    public ServiceRequestOfferResponse withdrawOffer(UUID providerUserId, UUID offerId) {
        ServiceRequestOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: " + offerId));

        validateOfferProviderOwnership(offer, providerUserId);

        if (offer.getStatus() != ServiceOfferStatus.PENDING) {
            throw new BadRequestException("Cannot withdraw offer with status: " + offer.getStatus());
        }

        offer.setStatus(ServiceOfferStatus.WITHDRAWN);
        ServiceRequestOffer saved = offerRepository.save(offer);
        log.info("Offer ID: {} withdrawn by provider user: {}", offerId, providerUserId);
        return mapper.toOfferResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public ServiceRequestResponse acceptOffer(UUID customerId, UUID requestId, UUID offerId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateCustomerOwnership(request, customerId);

        if (request.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BadRequestException("Cannot accept offer on request with status: " + request.getStatus() + ". Must be OPEN.");
        }

        ServiceRequestOffer targetOffer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: " + offerId));

        if (!targetOffer.getServiceRequest().getId().equals(requestId)) {
            throw new BadRequestException("Offer does not belong to this service request");
        }

        if (targetOffer.getStatus() != ServiceOfferStatus.PENDING) {
            throw new BadRequestException("Cannot accept offer with status: " + targetOffer.getStatus());
        }

        // Accept the selected offer
        targetOffer.setStatus(ServiceOfferStatus.ACCEPTED);
        offerRepository.save(targetOffer);

        // Reject other pending offers on this request
        List<ServiceRequestOffer> allOffers = offerRepository.findByServiceRequestId(requestId);
        for (ServiceRequestOffer o : allOffers) {
            if (!o.getId().equals(offerId) && o.getStatus() == ServiceOfferStatus.PENDING) {
                o.setStatus(ServiceOfferStatus.REJECTED);
                offerRepository.save(o);
                if (o.getProvider() != null && o.getProvider().getUser() != null) {
                    notificationService.sendNotification(
                            o.getProvider().getUser(),
                            NotificationType.OFFER_REJECTED,
                            "Offer Update",
                            "Your offer for '" + request.getTitle() + "' was not selected",
                            request.getId(),
                            "SERVICE_REQUEST"
                    );
                }
            }
        }

        request.setStatus(ServiceRequestStatus.ACCEPTED);
        request.setSelectedProvider(targetOffer.getProvider());
        ServiceRequest saved = requestRepository.save(request);

        // Automatically create/link confirmed Booking
        Instant scheduledAt = calculateScheduledAt(request.getPreferredDate(), request.getPreferredTime());

        Booking booking = Booking.builder()
                .customer(request.getCustomer())
                .provider(targetOffer.getProvider())
                .serviceRequest(request)
                .acceptedOffer(targetOffer)
                .price(targetOffer.getProposedPrice())
                .status(BookingStatus.CONFIRMED)
                .address(request.getAddress())
                .city(request.getCity())
                .scheduledDate(request.getPreferredDate())
                .scheduledStartTime(request.getPreferredTime())
                .scheduledAt(scheduledAt)
                .notes(request.getDescription())
                .build();
        bookingRepository.save(booking);

        // Notify selected provider
        if (targetOffer.getProvider() != null && targetOffer.getProvider().getUser() != null) {
            notificationService.sendNotification(
                    targetOffer.getProvider().getUser(),
                    NotificationType.OFFER_ACCEPTED,
                    "Offer Accepted! 🎉",
                    "Your offer of $" + targetOffer.getProposedPrice() + " for '" + request.getTitle() + "' was accepted!",
                    booking.getId(),
                    "BOOKING"
            );
        }

        log.info("Accepted offer ID: {} for service request ID: {} and created booking: {}", offerId, requestId, booking.getId());
        return mapper.toResponse(saved, customerId, UserRole.CUSTOMER, null);
    }

    @Override
    public ServiceRequestOfferResponse rejectOffer(UUID customerId, UUID requestId, UUID offerId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateCustomerOwnership(request, customerId);

        ServiceRequestOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: " + offerId));

        if (!offer.getServiceRequest().getId().equals(requestId)) {
            throw new BadRequestException("Offer does not belong to this service request");
        }

        if (offer.getStatus() != ServiceOfferStatus.PENDING) {
            throw new BadRequestException("Cannot reject offer with status: " + offer.getStatus());
        }

        offer.setStatus(ServiceOfferStatus.REJECTED);
        ServiceRequestOffer saved = offerRepository.save(offer);

        if (offer.getProvider() != null && offer.getProvider().getUser() != null) {
            notificationService.sendNotification(
                    offer.getProvider().getUser(),
                    NotificationType.OFFER_REJECTED,
                    "Offer Declined",
                    "Your offer for '" + request.getTitle() + "' was declined by customer",
                    request.getId(),
                    "SERVICE_REQUEST"
            );
        }

        log.info("Rejected offer ID: {} for service request ID: {} by customer: {}", offerId, requestId, customerId);
        return mapper.toOfferResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public ServiceRequestResponse startRequest(UUID providerUserId, UUID requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateSelectedProvider(request, providerUserId);

        if (request.getStatus() != ServiceRequestStatus.ACCEPTED) {
            throw new BadRequestException("Cannot start service request with status: " + request.getStatus() + ". Must be ACCEPTED.");
        }

        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        ServiceRequest saved = requestRepository.save(request);

        // Update linked booking if any
        bookingRepository.findAll((root, query, cb) -> cb.equal(root.get("serviceRequest").get("id"), requestId))
                .forEach(b -> {
                    b.setStatus(BookingStatus.IN_PROGRESS);
                    bookingRepository.save(b);
                });

        if (request.getCustomer() != null) {
            notificationService.sendNotification(
                    request.getCustomer(),
                    NotificationType.SERVICE_STARTED,
                    "Service Started 🔧",
                    "Provider has started work on '" + request.getTitle() + "'",
                    request.getId(),
                    "SERVICE_REQUEST"
            );
        }

        log.info("Service request ID: {} started by provider user: {}", requestId, providerUserId);
        return mapper.toResponse(saved, providerUserId, UserRole.PROVIDER, null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, key = "#requestId"),
            @CacheEvict(value = CacheNames.SERVICE_REQUESTS, allEntries = true)
    })
    public ServiceRequestResponse completeRequest(UUID providerUserId, UUID requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with ID: " + requestId));

        validateSelectedProvider(request, providerUserId);

        if (request.getStatus() != ServiceRequestStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot complete service request with status: " + request.getStatus() + ". Must be IN_PROGRESS.");
        }

        request.setStatus(ServiceRequestStatus.COMPLETED);
        ServiceRequest saved = requestRepository.save(request);

        // Update linked booking if any
        bookingRepository.findAll((root, query, cb) -> cb.equal(root.get("serviceRequest").get("id"), requestId))
                .forEach(b -> {
                    b.setStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(b);
                });

        // Increment provider completedServices
        ProviderProfile provider = request.getSelectedProvider();
        if (provider != null) {
            provider.setCompletedServices((provider.getCompletedServices() != null ? provider.getCompletedServices() : 0) + 1);
            providerProfileRepository.save(provider);
        }

        if (request.getCustomer() != null) {
            notificationService.sendNotification(
                    request.getCustomer(),
                    NotificationType.SERVICE_COMPLETED,
                    "Service Completed! 🎉",
                    "Provider has completed work on '" + request.getTitle() + "'. Please leave a review!",
                    request.getId(),
                    "SERVICE_REQUEST"
            );
        }

        log.info("Service request ID: {} completed by provider user: {}", requestId, providerUserId);
        return mapper.toResponse(saved, providerUserId, UserRole.PROVIDER, null);
    }

    private Instant calculateScheduledAt(LocalDate date, String timeStr) {
        if (date == null) {
            return null;
        }
        if (timeStr != null && !timeStr.isBlank()) {
            try {
                String cleanTime = timeStr.trim();
                if (cleanTime.matches("^\\d{1,2}:\\d{2}$")) {
                    String[] parts = cleanTime.split(":");
                    int hour = Integer.parseInt(parts[0]);
                    int min = Integer.parseInt(parts[1]);
                    return date.atTime(hour, min).atZone(ZoneOffset.UTC).toInstant();
                }
            } catch (Exception ignored) {
            }
        }
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private void validateCustomerOwnership(ServiceRequest request, UUID customerId) {
        if (request.getCustomer() == null || !request.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("You do not have permission to modify this service request");
        }
    }

    private void validateOfferProviderOwnership(ServiceRequestOffer offer, UUID providerUserId) {
        if (offer.getProvider() == null || offer.getProvider().getUser() == null ||
                !offer.getProvider().getUser().getId().equals(providerUserId)) {
            throw new ForbiddenException("You do not have permission to modify this offer");
        }
    }

    private void validateSelectedProvider(ServiceRequest request, UUID providerUserId) {
        if (request.getSelectedProvider() == null || request.getSelectedProvider().getUser() == null ||
                !request.getSelectedProvider().getUser().getId().equals(providerUserId)) {
            throw new ForbiddenException("You are not the selected provider for this service request");
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .toList();

        Sort validSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), validSort);
    }
}
