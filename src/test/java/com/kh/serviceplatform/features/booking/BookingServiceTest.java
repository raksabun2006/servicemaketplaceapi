package com.kh.serviceplatform.features.booking;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
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
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.service.ServiceOffer;
import com.kh.serviceplatform.features.service.ServiceRepository;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRequestOfferRepository serviceRequestOfferRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingMapper mapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User customer;
    private User providerUser;
    private ProviderProfile providerProfile;
    private ServiceOffer serviceOffer;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Customer Sok")
                .email("sok@example.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        providerUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Provider Dara")
                .email("dara@example.com")
                .role(UserRole.PROVIDER)
                .status(UserStatus.ACTIVE)
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .businessName("Dara Home Fix")
                .isAvailable(true)
                .build();

        serviceOffer = ServiceOffer.builder()
                .id(UUID.randomUUID())
                .provider(providerProfile)
                .name("Plumbing Inspection")
                .price(new BigDecimal("25.00"))
                .category(ServiceCategory.PLUMBING)
                .isAvailable(true)
                .build();

        booking = Booking.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .provider(providerProfile)
                .service(serviceOffer)
                .price(new BigDecimal("25.00"))
                .status(BookingStatus.PENDING)
                .address("St. 123, Phnom Penh")
                .scheduledAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();
    }

    @Test
    void shouldCreateBookingWithDatabaseSnapshotPrice() {
        CreateBookingRequest request = new CreateBookingRequest(
                serviceOffer.getId(),
                null,
                null,
                null,
                Instant.now().plus(2, ChronoUnit.DAYS),
                null,
                null,
                null,
                "St. 123, Phnom Penh",
                "Phnom Penh",
                "Please bring tools"
        );

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(any(Booking.class))).thenReturn(mock(BookingResponse.class));

        BookingResponse response = bookingService.createBooking(customer.getId(), request);

        assertNotNull(response);
        verify(bookingRepository).save(argThat(b ->
                b.getPrice().compareTo(serviceOffer.getPrice()) == 0 &&
                b.getStatus() == BookingStatus.PENDING &&
                b.getCustomer().equals(customer)
        ));
    }

    @Test
    void shouldPreventProviderFromBookingOwnService() {
        CreateBookingRequest request = new CreateBookingRequest(
                serviceOffer.getId(),
                null,
                null,
                null,
                Instant.now().plus(2, ChronoUnit.DAYS),
                null,
                null,
                null,
                "St. 123, Phnom Penh",
                "Phnom Penh",
                null
        );

        when(userRepository.findById(providerUser.getId())).thenReturn(Optional.of(providerUser));
        when(serviceRepository.findById(serviceOffer.getId())).thenReturn(Optional.of(serviceOffer));

        assertThrows(BadRequestException.class, () ->
                bookingService.createBooking(providerUser.getId(), request));
    }

    @Test
    void shouldAcceptBookingByAssignedProvider() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        bookingService.acceptBooking(providerUser.getId(), booking.getId());

        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldRejectAcceptingBookingByUnrelatedProvider() {
        UUID unrelatedProviderId = UUID.randomUUID();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () ->
                bookingService.acceptBooking(unrelatedProviderId, booking.getId()));
    }

    @Test
    void shouldRejectBookingByAssignedProvider() {
        RejectBookingRequest request = new RejectBookingRequest("Unavailable on scheduled time");
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        bookingService.rejectBooking(providerUser.getId(), booking.getId(), request);

        assertEquals(BookingStatus.REJECTED, booking.getStatus());
        assertEquals("Unavailable on scheduled time", booking.getRejectionReason());
    }

    @Test
    void shouldStartAcceptedBooking() {
        booking.setStatus(BookingStatus.ACCEPTED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        bookingService.startBooking(providerUser.getId(), booking.getId());

        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
    }

    @Test
    void shouldFailToStartPendingBooking() {
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                bookingService.startBooking(providerUser.getId(), booking.getId()));
    }

    @Test
    void shouldCompleteInProgressBooking() {
        booking.setStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        bookingService.completeBooking(providerUser.getId(), booking.getId());

        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    @Test
    void shouldCancelPendingOrAcceptedBooking() {
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        bookingService.cancelBooking(customer.getId(), UserRole.CUSTOMER, booking.getId(), new CancelBookingRequest("Changed plans"));

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void shouldFailToCancelCompletedBooking() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () ->
                bookingService.cancelBooking(customer.getId(), UserRole.CUSTOMER, booking.getId(), null));
    }

    @Test
    void shouldPreventUnrelatedCustomerFromViewingBooking() {
        UUID otherCustomerId = UUID.randomUUID();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () ->
                bookingService.getBookingById(otherCustomerId, UserRole.CUSTOMER, booking.getId()));
    }
}
