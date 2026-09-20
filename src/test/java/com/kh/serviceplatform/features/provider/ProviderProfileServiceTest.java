package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.favorite.FavoriteProviderRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.notification.NotificationRepository;
import com.kh.serviceplatform.features.provider.dto.ProviderDashboardResponse;
import com.kh.serviceplatform.features.provider.dto.ProviderProfileResponse;
import com.kh.serviceplatform.features.provider.dto.UpdateAvailabilityRequest;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderProfileServiceTest {

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceRequestOfferRepository serviceRequestOfferRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private FavoriteProviderRepository favoriteProviderRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProviderProfileMapper mapper;

    @InjectMocks
    private ProviderProfileServiceImpl providerProfileService;

    private User user;
    private ProviderProfile profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Dara Tech")
                .email("dara@example.com")
                .build();

        profile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .businessName("Dara Home Fix")
                .isAvailable(true)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .serviceRadiusKm(25.0)
                .workingDays("Mon,Tue,Wed,Thu,Fri")
                .workingHoursStart("08:00")
                .workingHoursEnd("18:00")
                .averageRating(4.8)
                .totalReviews(10)
                .completedServices(0)
                .verificationStatus(ProviderVerificationStatus.VERIFIED)
                .build();
    }

    @Test
    void shouldUpdateAvailability() {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(
                AvailabilityStatus.BUSY,
                "Mon,Tue,Wed",
                "09:00",
                "17:00",
                30.0
        );

        when(providerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(providerProfileRepository.save(any(ProviderProfile.class))).thenReturn(profile);
        when(mapper.toResponse(profile)).thenReturn(mock(ProviderProfileResponse.class));

        ProviderProfileResponse response = providerProfileService.updateAvailability(user.getId(), request);

        assertNotNull(response);
        assertEquals(AvailabilityStatus.BUSY, profile.getAvailabilityStatus());
        assertEquals(30.0, profile.getServiceRadiusKm());
        verify(providerProfileRepository).save(profile);
    }

    @Test
    void shouldGetProviderDashboard() {
        when(providerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(serviceRequestOfferRepository.countByProviderIdAndStatus(profile.getId(), ServiceOfferStatus.PENDING)).thenReturn(3L);
        when(serviceRequestRepository.countBySelectedProviderIdAndStatus(profile.getId(), ServiceRequestStatus.ACCEPTED)).thenReturn(5L);
        when(bookingRepository.countByProviderIdAndStatusIn(eq(profile.getId()), anyList())).thenReturn(0L);
        when(bookingRepository.countByProviderIdAndStatus(profile.getId(), BookingStatus.COMPLETED)).thenReturn(15L);
        when(bookingRepository.countByProviderIdAndStatus(profile.getId(), BookingStatus.CANCELLED)).thenReturn(1L);
        when(bookingRepository.countByProviderIdAndScheduledDate(profile.getId(), LocalDate.now())).thenReturn(2L);
        when(serviceRequestRepository.countByStatus(ServiceRequestStatus.OPEN)).thenReturn(8L);

        ProviderDashboardResponse dashboard = providerProfileService.getProviderDashboard(user.getId());

        assertNotNull(dashboard);
        assertEquals(8L, dashboard.newNearbyRequestsCount());
        assertEquals(3L, dashboard.pendingOffersCount());
        assertEquals(5L, dashboard.acceptedServicesCount());
        assertEquals(2L, dashboard.todayBookingsCount());
        assertEquals(15L, dashboard.completedServicesCount());
        assertEquals(1L, dashboard.cancelledServicesCount());
        assertEquals(4.8, dashboard.averageRating());
        assertEquals(10, dashboard.totalReviews());
    }
}
