package com.kh.serviceplatform.features.customer;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.customer.dto.CustomerDashboardResponse;
import com.kh.serviceplatform.features.favorite.FavoriteProviderRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.notification.NotificationRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestOfferRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRequestOfferRepository serviceRequestOfferRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FavoriteProviderRepository favoriteProviderRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CustomerProfileMapper mapper;

    @InjectMocks
    private CustomerProfileServiceImpl customerProfileService;

    private User user;
    private CustomerProfile profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Sok San")
                .email("sok@example.com")
                .build();

        profile = CustomerProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .preferredLanguage("en")
                .preferredCurrency("USD")
                .build();
    }

    @Test
    void shouldGetCustomerDashboard() {
        when(serviceRequestRepository.countByCustomerId(user.getId())).thenReturn(5L);
        when(serviceRequestRepository.countByCustomerIdAndStatus(user.getId(), ServiceRequestStatus.OPEN)).thenReturn(2L);
        when(serviceRequestRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(bookingRepository.countByCustomerIdAndStatusIn(eq(user.getId()), anyList())).thenReturn(1L);
        when(bookingRepository.countByCustomerIdAndStatus(user.getId(), BookingStatus.COMPLETED)).thenReturn(3L);
        when(serviceRequestRepository.countByCustomerIdAndStatus(user.getId(), ServiceRequestStatus.COMPLETED)).thenReturn(1L);
        when(serviceRequestRepository.countByCustomerIdAndStatus(user.getId(), ServiceRequestStatus.CANCELLED)).thenReturn(0L);
        when(bookingRepository.countByCustomerIdAndStatus(user.getId(), BookingStatus.CANCELLED)).thenReturn(0L);
        when(favoriteProviderRepository.countByCustomerId(user.getId())).thenReturn(4L);
        when(notificationRepository.countByRecipientIdAndReadFalse(user.getId())).thenReturn(1L);

        CustomerDashboardResponse dashboard = customerProfileService.getCustomerDashboard(user.getId());

        assertNotNull(dashboard);
        assertEquals(5L, dashboard.myRequestsCount());
        assertEquals(2L, dashboard.openRequestsCount());
        assertEquals(1L, dashboard.upcomingBookingsCount());
        assertEquals(4L, dashboard.completedServicesCount());
        assertEquals(4L, dashboard.favoriteProvidersCount());
        assertEquals(1L, dashboard.unreadNotificationsCount());
    }
}
