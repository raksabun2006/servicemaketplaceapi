package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.file.enums.FileType;
import com.kh.serviceplatform.features.provider.dto.*;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProviderProfileServiceImpl implements ProviderProfileService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "businessName", "experienceYears", "serviceArea", "hourlyRate",
            "isAvailable", "isVerified", "averageRating", "totalReviews", "completedServices", "createdAt", "updatedAt"
    );

    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestOfferRepository serviceRequestOfferRepository;
    private final BookingRepository bookingRepository;
    private final ProviderProfileMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getProfileByUserId(UUID userId) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        return mapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getProfileById(UUID id) {
        ProviderProfile profile = providerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found with ID: " + id));

        return mapper.toResponse(profile);
    }

    @Override
    public ProviderProfileResponse createProfile(UUID userId, CreateProviderProfileRequest request) {
        if (providerProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Provider profile already exists for this user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        ProviderProfile profile = ProviderProfile.builder()
                .user(user)
                .businessName(request.businessName().trim())
                .bio(request.bio() != null ? request.bio().trim() : null)
                .experienceYears(request.experienceYears())
                .serviceArea(request.serviceArea() != null ? request.serviceArea().trim() : null)
                .hourlyRate(request.hourlyRate())
                .isAvailable(true)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .serviceRadiusKm(10.0)
                .isVerified(false)
                .verificationStatus(ProviderVerificationStatus.UNVERIFIED)
                .averageRating(0.0)
                .totalReviews(0)
                .completedServices(0)
                .build();

        ProviderProfile saved = providerProfileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    @Override
    public ProviderProfileResponse updateProfile(UUID userId, UpdateProviderProfileRequest request) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        User user = profile.getUser();

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.phone() != null) {
            String trimmedPhone = request.phone().trim();
            if (!trimmedPhone.isBlank() && !trimmedPhone.equals(user.getPhone())) {
                if (userRepository.existsByPhone(trimmedPhone)) {
                    throw new BadRequestException("Phone number already registered");
                }
                user.setPhone(trimmedPhone);
            }
        }

        if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) {
            String url = request.avatarUrl().trim();
            String fileIdStr = url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : url;
            try {
                UUID fileId = UUID.fromString(fileIdStr);
                fileRepository.findById(fileId).ifPresent(user::setAvatarFile);
            } catch (IllegalArgumentException ignored) {
            }
        }

        userRepository.save(user);

        if (request.businessName() != null) {
            profile.setBusinessName(request.businessName().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().trim());
        }
        if (request.experienceYears() != null) {
            profile.setExperienceYears(request.experienceYears());
        }
        if (request.serviceArea() != null) {
            profile.setServiceArea(request.serviceArea().trim());
        }
        if (request.address() != null) {
            profile.setAddress(request.address().trim());
        }
        if (request.city() != null) {
            profile.setCity(request.city().trim());
        }
        if (request.district() != null) {
            profile.setDistrict(request.district().trim());
        }
        if (request.hourlyRate() != null) {
            profile.setHourlyRate(request.hourlyRate());
        }
        if (request.serviceRadiusKm() != null) {
            profile.setServiceRadiusKm(request.serviceRadiusKm());
        }
        if (request.workingDays() != null) {
            profile.setWorkingDays(request.workingDays().trim());
        }
        if (request.workingHoursStart() != null) {
            profile.setWorkingHoursStart(request.workingHoursStart().trim());
        }
        if (request.workingHoursEnd() != null) {
            profile.setWorkingHoursEnd(request.workingHoursEnd().trim());
        }
        if (request.availabilityStatus() != null) {
            profile.setAvailabilityStatus(request.availabilityStatus());
            profile.setAvailable(request.availabilityStatus() == AvailabilityStatus.AVAILABLE);
        } else if (request.isAvailable() != null) {
            profile.setAvailable(request.isAvailable());
            profile.setAvailabilityStatus(request.isAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.OFFLINE);
        }

        if (request.latitude() != null || request.longitude() != null) {
            Double lat = request.latitude() != null ? request.latitude() : profile.getLatitude();
            Double lon = request.longitude() != null ? request.longitude() : profile.getLongitude();
            if (lat != null && lon != null) {
                GeoUtils.validateCoordinates(lat, lon);
            }
            profile.setLatitude(request.latitude());
            profile.setLongitude(request.longitude());
        }
        if (request.identityDocumentFileId() != null) {
            StoredFile idDoc = validateAndGetFile(request.identityDocumentFileId(), userId, FileType.PROVIDER_DOCUMENT, "Identity document");
            profile.setIdentityDocumentFile(idDoc);
        }
        if (request.profilePhotoFileId() != null) {
            StoredFile photo = validateAndGetFile(request.profilePhotoFileId(), userId, FileType.AVATAR, "Profile photo");
            profile.setProfilePhotoFile(photo);
        }

        ProviderProfile saved = providerProfileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    @Override
    public ProviderProfileResponse updateAvailability(UUID userId, UpdateAvailabilityRequest request) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        if (request.availabilityStatus() != null) {
            profile.setAvailabilityStatus(request.availabilityStatus());
            profile.setAvailable(request.availabilityStatus() == AvailabilityStatus.AVAILABLE);
        }
        if (request.workingDays() != null) {
            profile.setWorkingDays(request.workingDays().trim());
        }
        if (request.workingHoursStart() != null) {
            profile.setWorkingHoursStart(request.workingHoursStart().trim());
        }
        if (request.workingHoursEnd() != null) {
            profile.setWorkingHoursEnd(request.workingHoursEnd().trim());
        }
        if (request.serviceRadiusKm() != null) {
            GeoUtils.validateRadius(request.serviceRadiusKm());
            profile.setServiceRadiusKm(request.serviceRadiusKm());
        }

        ProviderProfile saved = providerProfileRepository.save(profile);
        log.info("Updated provider availability for user {}: status={}, radius={}", userId, profile.getAvailabilityStatus(), profile.getServiceRadiusKm());
        return mapper.toResponse(saved);
    }

    @Override
    public ProviderProfileResponse submitVerification(UUID userId, ProviderVerificationRequest request) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        StoredFile idDoc = validateAndGetFile(request.identityDocumentFileId(), userId, FileType.PROVIDER_DOCUMENT, "Identity document");
        StoredFile profilePhoto = validateAndGetFile(request.profilePhotoFileId(), userId, FileType.AVATAR, "Profile photo");

        profile.setIdentityDocumentFile(idDoc);
        profile.setProfilePhotoFile(profilePhoto);
        profile.setVerificationStatus(ProviderVerificationStatus.PENDING);
        profile.setRejectionReason(null);

        ProviderProfile saved = providerProfileRepository.save(profile);
        log.info("Provider verification request submitted for user: {}", userId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProviderProfileResponse> getAvailableProviders(Boolean availableNow, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);

        if (Boolean.TRUE.equals(availableNow)) {
            Page<ProviderProfile> availablePage = providerProfileRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE, safePageable);
            return availablePage.map(mapper::toResponse);
        }

        return providerProfileRepository.findAll(safePageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NearbyProviderResponse> getNearbyProviders(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        GeoUtils.validateCoordinates(latitude, longitude);
        double maxRadius = radiusKm != null ? radiusKm : 10.0;
        GeoUtils.validateRadius(maxRadius);

        List<ProviderProfile> availableProviders = providerProfileRepository.findAvailableWithCoordinates();
        List<NearbyProviderResponse> nearbyList = new ArrayList<>();

        for (ProviderProfile provider : availableProviders) {
            double distance = GeoUtils.calculateDistanceKm(latitude, longitude, provider.getLatitude(), provider.getLongitude());
            if (distance <= maxRadius) {
                nearbyList.add(mapper.toNearbyResponse(provider, distance));
            }
        }

        // Sort by distance ascending
        nearbyList.sort(Comparator.comparing(NearbyProviderResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));

        int page = pageable != null ? pageable.getPageNumber() : 0;
        int size = pageable != null && pageable.getPageSize() > 0 ? pageable.getPageSize() : 20;

        int fromIndex = Math.min(page * size, nearbyList.size());
        int toIndex = Math.min(fromIndex + size, nearbyList.size());
        List<NearbyProviderResponse> pageContent = nearbyList.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, pageable != null ? pageable : PageRequest.of(0, 20), nearbyList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderDashboardResponse getProviderDashboard(UUID userId) {
        ProviderProfile profile = providerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        long pendingOffers = serviceRequestOfferRepository.countByProviderIdAndStatus(profile.getId(), ServiceOfferStatus.PENDING);
        long acceptedServices = serviceRequestRepository.countBySelectedProviderIdAndStatus(profile.getId(), ServiceRequestStatus.ACCEPTED)
                + bookingRepository.countByProviderIdAndStatusIn(profile.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS));
        long todayBookings = bookingRepository.countByProviderIdAndScheduledDate(profile.getId(), LocalDate.now());
        long completedServices = bookingRepository.countByProviderIdAndStatus(profile.getId(), BookingStatus.COMPLETED)
                + (profile.getCompletedServices() != null ? profile.getCompletedServices() : 0);
        long cancelledServices = bookingRepository.countByProviderIdAndStatus(profile.getId(), BookingStatus.CANCELLED);

        // Calculate nearby open requests
        long newNearbyRequestsCount = 0;
        if (profile.getLatitude() != null && profile.getLongitude() != null) {
            List<ServiceRequest> openWithCoords = serviceRequestRepository.findByStatusAndHasCoordinates(ServiceRequestStatus.OPEN);
            double radius = profile.getServiceRadiusKm() != null ? profile.getServiceRadiusKm() : 10.0;
            newNearbyRequestsCount = openWithCoords.stream()
                    .filter(sr -> GeoUtils.calculateDistanceKm(profile.getLatitude(), profile.getLongitude(), sr.getLatitude(), sr.getLongitude()) <= radius)
                    .count();
        } else {
            newNearbyRequestsCount = serviceRequestRepository.countByStatus(ServiceRequestStatus.OPEN);
        }

        return new ProviderDashboardResponse(
                newNearbyRequestsCount,
                pendingOffers,
                acceptedServices,
                todayBookings,
                completedServices,
                cancelledServices,
                profile.getAverageRating() != null ? profile.getAverageRating() : 0.0,
                profile.getTotalReviews() != null ? profile.getTotalReviews() : 0
        );
    }

    @Override
    public ProviderProfile createDefaultProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        ProviderProfile profile = ProviderProfile.builder()
                .user(user)
                .businessName(user.getFullName())
                .isAvailable(true)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .serviceRadiusKm(10.0)
                .isVerified(false)
                .verificationStatus(ProviderVerificationStatus.UNVERIFIED)
                .averageRating(0.0)
                .totalReviews(0)
                .completedServices(0)
                .build();

        return providerProfileRepository.save(profile);
    }

    private StoredFile validateAndGetFile(UUID fileId, UUID currentUserId, FileType expectedType, String fieldName) {
        if (fileId == null) {
            return null;
        }

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException(fieldName + " not found with ID: " + fileId));

        if (!file.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not own the uploaded " + fieldName);
        }

        if (file.getFileType() != expectedType) {
            throw new BadRequestException(fieldName + " must be of type " + expectedType + " but was " + file.getFileType());
        }

        return file;
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
