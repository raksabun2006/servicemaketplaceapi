package com.kh.serviceplatform.features.review;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.booking.Booking;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.review.dto.CreateReviewRequest;
import com.kh.serviceplatform.features.review.dto.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReviewMapper mapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User customer;
    private ProviderProfile providerProfile;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Customer Sok")
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .businessName("Dara Home Fix")
                .averageRating(0.0)
                .totalReviews(0)
                .build();

        booking = Booking.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .provider(providerProfile)
                .status(BookingStatus.COMPLETED)
                .build();
    }

    @Test
    void shouldCreateReviewForCompletedBookingAndRecalculateProviderRating() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Superb service!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(false);

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .customer(customer)
                .provider(providerProfile)
                .rating(5)
                .comment("Superb service!")
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingByProviderId(providerProfile.getId())).thenReturn(5.0);
        when(reviewRepository.countByProviderId(providerProfile.getId())).thenReturn(1L);
        when(mapper.toResponse(review)).thenReturn(mock(ReviewResponse.class));

        ReviewResponse response = reviewService.createReview(customer.getId(), booking.getId(), request);

        assertNotNull(response);
        verify(reviewRepository).save(any(Review.class));
        verify(providerProfileRepository).save(argThat(p ->
                p.getAverageRating() == 5.0 && p.getTotalReviews() == 1
        ));
    }

    @Test
    void shouldRejectDuplicateReviewForSameBooking() {
        CreateReviewRequest request = new CreateReviewRequest(4, "Good service");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                reviewService.createReview(customer.getId(), booking.getId(), request));
    }

    @Test
    void shouldRejectReviewForNonCompletedBooking() {
        booking.setStatus(BookingStatus.ACCEPTED);
        CreateReviewRequest request = new CreateReviewRequest(5, "Nice");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                reviewService.createReview(customer.getId(), booking.getId(), request));
    }

    @Test
    void shouldRejectReviewByUnrelatedCustomer() {
        UUID otherCustomerId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(5, "Nice");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () ->
                reviewService.createReview(otherCustomerId, booking.getId(), request));
    }
}
