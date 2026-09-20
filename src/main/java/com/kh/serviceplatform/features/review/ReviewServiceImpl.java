package com.kh.serviceplatform.features.review;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.review.dto.CreateReviewRequest;
import com.kh.serviceplatform.features.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "rating", "createdAt", "updatedAt"
    );

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final NotificationService notificationService;
    private final ReviewMapper mapper;

    @Override
    public ReviewResponse createReview(UUID customerUserId, UUID bookingId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerUserId)) {
            throw new ForbiddenException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED bookings can be reviewed. Current status is " + booking.getStatus());
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new BadRequestException("A review has already been submitted for this booking");
        }

        ProviderProfile provider = booking.getProvider();

        Review review = Review.builder()
                .booking(booking)
                .customer(booking.getCustomer())
                .provider(provider)
                .rating(request.rating())
                .comment(request.comment() != null ? request.comment().trim() : null)
                .build();

        Review saved = reviewRepository.save(review);

        // Update provider statistics
        if (provider != null) {
            Double avgRating = reviewRepository.calculateAverageRatingByProviderId(provider.getId());
            long totalReviews = reviewRepository.countByProviderId(provider.getId());
            provider.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
            provider.setTotalReviews((int) totalReviews);
            providerProfileRepository.save(provider);

            if (provider.getUser() != null) {
                notificationService.sendNotification(
                        provider.getUser(),
                        NotificationType.REVIEW_RECEIVED,
                        "New Review Received ⭐ (" + request.rating() + "/5)",
                        "A customer left a " + request.rating() + "-star review for your service" + (request.comment() != null ? ": \"" + request.comment() + "\"" : "."),
                        saved.getId(),
                        "REVIEW"
                );
            }
        }

        log.info("Created review ID: {} for booking ID: {} with rating: {}", saved.getId(), bookingId, request.rating());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProviderId(UUID providerId, Pageable pageable) {
        if (!providerProfileRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found with ID: " + providerId);
        }

        Pageable safePageable = sanitizePageable(pageable);
        return reviewRepository.findByProviderId(providerId, safePageable)
                .map(mapper::toResponse);
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
