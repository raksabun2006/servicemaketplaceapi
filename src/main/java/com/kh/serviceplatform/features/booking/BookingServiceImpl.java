package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.booking.dto.CancelBookingRequest;
import com.kh.serviceplatform.features.booking.dto.CreateBookingRequest;
import com.kh.serviceplatform.features.booking.dto.RejectBookingRequest;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.service.ServiceOffer;
import com.kh.serviceplatform.features.service.ServiceRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequest;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOffer;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "price", "status", "scheduledAt", "scheduledDate", "createdAt", "updatedAt"
    );

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestOfferRepository serviceRequestOfferRepository;
    private final NotificationService notificationService;
    private final BookingMapper mapper;

    @Override
    public BookingResponse createBooking(UUID customerId, CreateBookingRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active");
        }

        ServiceOffer service = null;
        ProviderProfile provider = null;
        BigDecimal price = BigDecimal.ZERO;
        ServiceRequest serviceRequest = null;
        ServiceRequestOffer acceptedOffer = null;

        if (request.serviceId() != null) {
            service = serviceRepository.findById(request.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + request.serviceId()));
            if (!service.isAvailable()) {
                throw new BadRequestException("Service is currently not available for booking");
            }
            provider = service.getProvider();
            price = service.getPrice();
        } else if (request.providerId() != null) {
            provider = providerProfileRepository.findById(request.providerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found with ID: " + request.providerId()));
            price = provider.getHourlyRate() != null ? provider.getHourlyRate() : BigDecimal.ZERO;
        }

        if (request.serviceRequestId() != null) {
            serviceRequest = serviceRequestRepository.findById(request.serviceRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + request.serviceRequestId()));
            if (provider == null && serviceRequest.getSelectedProvider() != null) {
                provider = serviceRequest.getSelectedProvider();
            }
        }

        if (request.acceptedOfferId() != null) {
            acceptedOffer = serviceRequestOfferRepository.findById(request.acceptedOfferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + request.acceptedOfferId()));
            if (provider == null) {
                provider = acceptedOffer.getProvider();
            }
            price = acceptedOffer.getProposedPrice();
        }

        if (provider == null) {
            throw new BadRequestException("A valid service or provider must be specified for booking");
        }

        if (provider.getUser() != null && provider.getUser().getId().equals(customerId)) {
            throw new BadRequestException("Providers cannot book their own services");
        }

        Instant scheduledAt = request.scheduledAt();
        if (scheduledAt == null && request.scheduledDate() != null) {
            scheduledAt = calculateScheduledAt(request.scheduledDate(), request.scheduledStartTime());
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .provider(provider)
                .service(service)
                .serviceRequest(serviceRequest)
                .acceptedOffer(acceptedOffer)
                .price(price)
                .status(BookingStatus.PENDING)
                .address(request.address().trim())
                .city(request.city() != null ? request.city().trim() : null)
                .scheduledAt(scheduledAt)
                .scheduledDate(request.scheduledDate())
                .scheduledStartTime(request.scheduledStartTime())
                .scheduledEndTime(request.scheduledEndTime())
                .notes(request.notes() != null ? request.notes().trim() : null)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking ID: {} for customer: {} and provider: {}", saved.getId(), customerId, provider.getId());

        // Notify provider
        if (provider.getUser() != null) {
            notificationService.sendNotification(
                    provider.getUser(),
                    NotificationType.NEW_OFFER,
                    "New Booking Request 📅",
                    customer.getFullName() + " requested a new booking for " + (service != null ? service.getName() : "services"),
                    saved.getId(),
                    "BOOKING"
            );
        }

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(UUID userId, UserRole role, BookingStatus status, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role == UserRole.CUSTOMER) {
                predicates.add(cb.equal(root.get("customer").get("id"), userId));
            } else if (role == UserRole.PROVIDER) {
                predicates.add(cb.equal(root.get("provider").get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookingRepository.findAll(spec, safePageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID userId, UserRole role, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateAccess(booking, userId, role);
        return mapper.toResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(UUID userId, UserRole role, UUID bookingId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateAccess(booking, userId, role);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACCEPTED && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel booking with current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (request != null && request.reason() != null) {
            booking.setCancellationReason(request.reason().trim());
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking ID: {} cancelled by user: {}", bookingId, userId);

        // Notify other participant
        boolean isCustomer = booking.getCustomer() != null && booking.getCustomer().getId().equals(userId);
        User recipient = isCustomer ? (booking.getProvider() != null ? booking.getProvider().getUser() : null) : booking.getCustomer();
        if (recipient != null) {
            notificationService.sendNotification(
                    recipient,
                    NotificationType.BOOKING_CANCELLED,
                    "Booking Cancelled",
                    "Booking #" + booking.getId() + " has been cancelled" + (booking.getCancellationReason() != null ? ": " + booking.getCancellationReason() : ""),
                    booking.getId(),
                    "BOOKING"
            );
        }

        return mapper.toResponse(saved);
    }

    @Override
    public BookingResponse acceptBooking(UUID providerUserId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateProviderOwnership(booking, providerUserId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Cannot accept booking with status: " + booking.getStatus() + ". Must be PENDING.");
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking ID: {} accepted by provider user: {}", bookingId, providerUserId);

        // Notify customer
        if (booking.getCustomer() != null) {
            notificationService.sendNotification(
                    booking.getCustomer(),
                    NotificationType.BOOKING_CONFIRMED,
                    "Booking Confirmed! ✅",
                    "Your booking has been accepted and confirmed by the provider.",
                    booking.getId(),
                    "BOOKING"
            );
        }

        return mapper.toResponse(saved);
    }

    @Override
    public BookingResponse rejectBooking(UUID providerUserId, UUID bookingId, RejectBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateProviderOwnership(booking, providerUserId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Cannot reject booking with status: " + booking.getStatus() + ". Must be PENDING.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        if (request != null && request.reason() != null) {
            booking.setRejectionReason(request.reason().trim());
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking ID: {} rejected by provider user: {}", bookingId, providerUserId);

        // Notify customer
        if (booking.getCustomer() != null) {
            notificationService.sendNotification(
                    booking.getCustomer(),
                    NotificationType.BOOKING_REJECTED,
                    "Booking Declined",
                    "Your booking request was declined by the provider" + (booking.getRejectionReason() != null ? ": " + booking.getRejectionReason() : ""),
                    booking.getId(),
                    "BOOKING"
            );
        }

        return mapper.toResponse(saved);
    }

    @Override
    public BookingResponse startBooking(UUID providerUserId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateProviderOwnership(booking, providerUserId);

        if (booking.getStatus() != BookingStatus.ACCEPTED && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot start booking with status: " + booking.getStatus() + ". Must be ACCEPTED or CONFIRMED.");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        Booking saved = bookingRepository.save(booking);

        if (booking.getServiceRequest() != null) {
            ServiceRequest req = booking.getServiceRequest();
            req.setStatus(ServiceRequestStatus.IN_PROGRESS);
            serviceRequestRepository.save(req);
        }

        if (booking.getCustomer() != null) {
            notificationService.sendNotification(
                    booking.getCustomer(),
                    NotificationType.SERVICE_STARTED,
                    "Service Started 🔧",
                    "Provider has started service for your booking.",
                    booking.getId(),
                    "BOOKING"
            );
        }

        log.info("Booking ID: {} started by provider user: {}", bookingId, providerUserId);
        return mapper.toResponse(saved);
    }

    @Override
    public BookingResponse completeBooking(UUID providerUserId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateProviderOwnership(booking, providerUserId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot complete booking with status: " + booking.getStatus() + ". Must be IN_PROGRESS.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        // Update linked service request if any
        if (booking.getServiceRequest() != null) {
            ServiceRequest req = booking.getServiceRequest();
            req.setStatus(ServiceRequestStatus.COMPLETED);
            serviceRequestRepository.save(req);
        }

        // Increment provider completed services
        ProviderProfile provider = booking.getProvider();
        if (provider != null) {
            provider.setCompletedServices((provider.getCompletedServices() != null ? provider.getCompletedServices() : 0) + 1);
            providerProfileRepository.save(provider);
        }

        if (booking.getCustomer() != null) {
            notificationService.sendNotification(
                    booking.getCustomer(),
                    NotificationType.SERVICE_COMPLETED,
                    "Service Completed! 🎉",
                    "Your booking has been completed. Please rate and review your experience!",
                    booking.getId(),
                    "BOOKING"
            );
        }

        log.info("Booking ID: {} completed by provider user: {}", bookingId, providerUserId);
        return mapper.toResponse(saved);
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

    private void validateAccess(Booking booking, UUID userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return;
        }

        boolean isCustomer = booking.getCustomer() != null && booking.getCustomer().getId().equals(userId);
        boolean isProvider = booking.getProvider() != null && booking.getProvider().getUser() != null &&
                booking.getProvider().getUser().getId().equals(userId);

        if (!isCustomer && !isProvider) {
            throw new ForbiddenException("You do not have permission to access this booking");
        }
    }

    private void validateProviderOwnership(Booking booking, UUID providerUserId) {
        if (booking.getProvider() == null || booking.getProvider().getUser() == null ||
                !booking.getProvider().getUser().getId().equals(providerUserId)) {
            throw new ForbiddenException("You are not the assigned provider for this booking");
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
