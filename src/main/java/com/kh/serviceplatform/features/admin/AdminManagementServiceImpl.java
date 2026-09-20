package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.admin.dto.AdminAnalyticsResponse;
import com.kh.serviceplatform.features.admin.dto.AdminDashboardResponse;
import com.kh.serviceplatform.features.admin.dto.ProviderReviewActionRequest;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserMapper;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingMapper;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileMapper;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.dto.ProviderProfileResponse;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import com.kh.serviceplatform.features.service.ServiceRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final Set<String> ALLOWED_USER_SORTS = Set.of(
            "id", "fullName", "email", "phone", "role", "status", "createdAt"
    );

    private static final Set<String> ALLOWED_PROVIDER_SORTS = Set.of(
            "id", "businessName", "experienceYears", "hourlyRate", "isAvailable", "isVerified", "verificationStatus", "averageRating", "totalReviews", "createdAt"
    );

    private static final Set<String> ALLOWED_BOOKING_SORTS = Set.of(
            "id", "price", "status", "scheduledAt", "createdAt"
    );

    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final ProviderProfileMapper providerProfileMapper;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, UserRole role, UserStatus status, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, ALLOWED_USER_SORTS);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, safePageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProviderProfileResponse> getProviders(
            String search,
            ProviderVerificationStatus verificationStatus,
            Boolean isVerified,
            Pageable pageable
    ) {
        Pageable safePageable = sanitizePageable(pageable, ALLOWED_PROVIDER_SORTS);

        Specification<ProviderProfile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }

            if (isVerified != null) {
                predicates.add(cb.equal(root.get("isVerified"), isVerified));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate businessNameMatch = cb.like(cb.lower(root.get("businessName")), pattern);
                Predicate serviceAreaMatch = cb.like(cb.lower(root.get("serviceArea")), pattern);
                Predicate userFullNameMatch = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                predicates.add(cb.or(businessNameMatch, serviceAreaMatch, userFullNameMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return providerProfileRepository.findAll(spec, safePageable)
                .map(providerProfileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProviderProfileResponse> getPendingProviders(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, ALLOWED_PROVIDER_SORTS);
        return providerProfileRepository.findByVerificationStatus(ProviderVerificationStatus.PENDING, safePageable)
                .map(providerProfileMapper::toResponse);
    }

    @Override
    public ProviderProfileResponse approveProvider(UUID providerId, ProviderReviewActionRequest request) {
        ProviderProfile profile = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found with ID: " + providerId));

        profile.setVerified(true);
        profile.setVerificationStatus(ProviderVerificationStatus.VERIFIED);
        profile.setRejectionReason(null);

        User user = profile.getUser();
        if (user != null && user.getRole() != UserRole.PROVIDER && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.PROVIDER);
            userRepository.save(user);
        }

        ProviderProfile saved = providerProfileRepository.save(profile);
        log.info("Provider profile ID: {} approved by admin", providerId);

        if (user != null) {
            notificationService.sendNotification(
                    user,
                    NotificationType.VERIFICATION_APPROVED,
                    "Verification Approved! 🎉",
                    "Your provider profile has been verified! You now display a verified badge to customers.",
                    profile.getId(),
                    "PROVIDER_PROFILE"
            );
        }

        return providerProfileMapper.toResponse(saved);
    }

    @Override
    public ProviderProfileResponse rejectProvider(UUID providerId, ProviderReviewActionRequest request) {
        ProviderProfile profile = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found with ID: " + providerId));

        profile.setVerified(false);
        profile.setVerificationStatus(ProviderVerificationStatus.REJECTED);
        String reason = (request != null && request.reason() != null) ? request.reason().trim() : "Documents could not be verified";
        profile.setRejectionReason(reason);

        ProviderProfile saved = providerProfileRepository.save(profile);
        log.info("Provider profile ID: {} rejected by admin. Reason: {}", providerId, reason);

        User user = profile.getUser();
        if (user != null) {
            notificationService.sendNotification(
                    user,
                    NotificationType.VERIFICATION_REJECTED,
                    "Verification Request Update",
                    "Your provider verification was not approved. Reason: " + reason,
                    profile.getId(),
                    "PROVIDER_PROFILE"
            );
        }

        return providerProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(BookingStatus status, String search, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, ALLOWED_BOOKING_SORTS);

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate customerName = cb.like(cb.lower(root.get("customer").get("fullName")), pattern);
                Predicate serviceName = cb.like(cb.lower(root.get("service").get("name")), pattern);
                Predicate providerBusinessName = cb.like(cb.lower(root.get("provider").get("businessName")), pattern);
                predicates.add(cb.or(customerName, serviceName, providerBusinessName));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookingRepository.findAll(spec, safePageable)
                .map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long totalProviders = userRepository.countByRole(UserRole.PROVIDER);
        long totalServices = serviceRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        BigDecimal totalRevenue = bookingRepository.sumPriceByStatus(BookingStatus.COMPLETED);
        long pendingProviderVerifications = providerProfileRepository.countByVerificationStatus(ProviderVerificationStatus.PENDING);

        return new AdminDashboardResponse(
                totalUsers,
                totalCustomers,
                totalProviders,
                totalServices,
                totalBookings,
                pendingBookings,
                completedBookings,
                cancelledBookings,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                pendingProviderVerifications
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long totalProviders = userRepository.countByRole(UserRole.PROVIDER);
        long verifiedProviders = providerProfileRepository.countByIsVerifiedTrue();
        long pendingProviderVerifications = providerProfileRepository.countByVerificationStatus(ProviderVerificationStatus.PENDING);

        long openServiceRequests = serviceRequestRepository.countByStatus(ServiceRequestStatus.OPEN);
        long totalServiceRequests = serviceRequestRepository.count();
        long completedServices = serviceRequestRepository.countByStatus(ServiceRequestStatus.COMPLETED);
        long cancelledServices = serviceRequestRepository.countByStatus(ServiceRequestStatus.CANCELLED);

        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        BigDecimal totalRevenue = bookingRepository.sumCompletedRevenue();

        Double avgRating = providerProfileRepository.getAverageMarketplaceRating();
        double averageRating = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        Map<String, Long> popularCategories = new LinkedHashMap<>();
        for (Object[] row : serviceRequestRepository.countRequestsByCategory()) {
            if (row != null && row.length == 2 && row[0] != null) {
                popularCategories.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> popularLocations = new LinkedHashMap<>();
        for (Object[] row : serviceRequestRepository.countRequestsByCity()) {
            if (row != null && row.length == 2 && row[0] != null) {
                popularLocations.put(row[0].toString(), (Long) row[1]);
            }
        }

        return new AdminAnalyticsResponse(
                totalUsers,
                totalCustomers,
                totalProviders,
                verifiedProviders,
                pendingProviderVerifications,
                openServiceRequests,
                totalServiceRequests,
                completedServices,
                cancelledServices,
                totalBookings,
                pendingBookings,
                completedBookings,
                cancelledBookings,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                averageRating,
                popularCategories,
                popularLocations
        );
    }

    private Pageable sanitizePageable(Pageable pageable, Set<String> allowedProperties) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> allowedProperties.contains(order.getProperty()))
                .toList();

        Sort validSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), validSort);
    }
}
