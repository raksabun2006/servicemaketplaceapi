package com.kh.serviceplatform.features.customer;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.customer.dto.CustomerDashboardResponse;
import com.kh.serviceplatform.features.customer.dto.CustomerProfileResponse;
import com.kh.serviceplatform.features.customer.dto.UpdateCustomerProfileRequest;
import com.kh.serviceplatform.features.favorite.FavoriteProviderRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.notification.NotificationRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestOfferRepository serviceRequestOfferRepository;
    private final BookingRepository bookingRepository;
    private final FavoriteProviderRepository favoriteProviderRepository;
    private final NotificationRepository notificationRepository;
    private final CustomerProfileMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfileByUserId(UUID userId) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        return mapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfileById(UUID id) {
        CustomerProfile profile = customerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found with ID: " + id));

        return mapper.toResponse(profile);
    }

    @Override
    public CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
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

        if (request.preferredLanguage() != null) {
            profile.setPreferredLanguage(request.preferredLanguage().trim());
        }
        if (request.preferredCurrency() != null) {
            profile.setPreferredCurrency(request.preferredCurrency().trim());
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
        if (request.postalCode() != null) {
            profile.setPostalCode(request.postalCode().trim());
        }
        if (request.notes() != null) {
            profile.setNotes(request.notes().trim());
        }

        CustomerProfile saved = customerProfileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardResponse getCustomerDashboard(UUID userId) {
        long myRequestsCount = serviceRequestRepository.countByCustomerId(userId);
        long openRequestsCount = serviceRequestRepository.countByCustomerIdAndStatus(userId, ServiceRequestStatus.OPEN);

        List<ServiceRequest> userOpenRequests = serviceRequestRepository.findAll((root, query, cb) ->
                cb.and(cb.equal(root.get("customer").get("id"), userId), cb.equal(root.get("status"), ServiceRequestStatus.OPEN)));

        long pendingOffersCount = 0;
        for (ServiceRequest req : userOpenRequests) {
            pendingOffersCount += serviceRequestOfferRepository.countByServiceRequestId(req.getId());
        }

        long upcomingBookingsCount = bookingRepository.countByCustomerIdAndStatusIn(
                userId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACCEPTED)
        );
        long completedServicesCount = bookingRepository.countByCustomerIdAndStatus(userId, BookingStatus.COMPLETED)
                + serviceRequestRepository.countByCustomerIdAndStatus(userId, ServiceRequestStatus.COMPLETED);
        long cancelledRequestsCount = serviceRequestRepository.countByCustomerIdAndStatus(userId, ServiceRequestStatus.CANCELLED)
                + bookingRepository.countByCustomerIdAndStatus(userId, BookingStatus.CANCELLED);
        long favoriteProvidersCount = favoriteProviderRepository.countByCustomerId(userId);
        long unreadNotificationsCount = notificationRepository.countByRecipientIdAndReadFalse(userId);

        return new CustomerDashboardResponse(
                myRequestsCount,
                openRequestsCount,
                pendingOffersCount,
                upcomingBookingsCount,
                completedServicesCount,
                cancelledRequestsCount,
                favoriteProvidersCount,
                unreadNotificationsCount
        );
    }

    @Override
    public CustomerProfile createDefaultProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        CustomerProfile profile = CustomerProfile.builder()
                .user(user)
                .preferredLanguage("en")
                .preferredCurrency("USD")
                .build();

        return customerProfileRepository.save(profile);
    }
}
