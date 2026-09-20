package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.features.admin.dto.AdminDashboardResponse;
import com.kh.serviceplatform.features.admin.dto.ProviderReviewActionRequest;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserMapper;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.booking.BookingRepository;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.notification.NotificationService;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileMapper;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.dto.ProviderProfileResponse;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import com.kh.serviceplatform.features.service.ServiceRepository;
import com.kh.serviceplatform.features.servicerequest.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProviderProfileMapper providerProfileMapper;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    private User applicantUser;
    private ProviderProfile providerProfile;

    @BeforeEach
    void setUp() {
        applicantUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Applicant Chan")
                .email("chan@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        providerProfile = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(applicantUser)
                .businessName("Chan Cleaners")
                .isVerified(false)
                .verificationStatus(ProviderVerificationStatus.PENDING)
                .build();
    }

    @Test
    void shouldApproveProviderAndUpgradeUserRole() {
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(providerProfileRepository.save(any(ProviderProfile.class))).thenReturn(providerProfile);
        when(providerProfileMapper.toResponse(any(ProviderProfile.class))).thenReturn(mock(ProviderProfileResponse.class));

        adminManagementService.approveProvider(providerProfile.getId(), new ProviderReviewActionRequest("Verified"));

        assertTrue(providerProfile.isVerified());
        assertEquals(ProviderVerificationStatus.VERIFIED, providerProfile.getVerificationStatus());
        assertEquals(UserRole.PROVIDER, applicantUser.getRole());
        verify(userRepository).save(applicantUser);
        verify(providerProfileRepository).save(providerProfile);
        verify(notificationService).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectProviderWithReason() {
        when(providerProfileRepository.findById(providerProfile.getId())).thenReturn(Optional.of(providerProfile));
        when(providerProfileRepository.save(any(ProviderProfile.class))).thenReturn(providerProfile);
        when(providerProfileMapper.toResponse(any(ProviderProfile.class))).thenReturn(mock(ProviderProfileResponse.class));

        adminManagementService.rejectProvider(providerProfile.getId(), new ProviderReviewActionRequest("ID illegible"));

        assertFalse(providerProfile.isVerified());
        assertEquals(ProviderVerificationStatus.REJECTED, providerProfile.getVerificationStatus());
        assertEquals("ID illegible", providerProfile.getRejectionReason());
        verify(providerProfileRepository).save(providerProfile);
        verify(notificationService).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldListUsers() {
        Page<User> userPage = new PageImpl<>(List.of(applicantUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toResponse(any(User.class))).thenReturn(mock(UserResponse.class));

        Page<UserResponse> result = adminManagementService.getUsers("chan", null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldGetDashboardStats() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(UserRole.CUSTOMER)).thenReturn(80L);
        when(userRepository.countByRole(UserRole.PROVIDER)).thenReturn(20L);
        when(serviceRepository.count()).thenReturn(50L);
        when(bookingRepository.count()).thenReturn(200L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(10L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(150L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(20L);
        when(bookingRepository.sumPriceByStatus(BookingStatus.COMPLETED)).thenReturn(new BigDecimal("7500.00"));
        when(providerProfileRepository.countByVerificationStatus(ProviderVerificationStatus.PENDING)).thenReturn(5L);

        AdminDashboardResponse dashboard = adminManagementService.getDashboard();

        assertNotNull(dashboard);
        assertEquals(100, dashboard.totalUsers());
        assertEquals(80, dashboard.totalCustomers());
        assertEquals(20, dashboard.totalProviders());
        assertEquals(50, dashboard.totalServices());
        assertEquals(200, dashboard.totalBookings());
        assertEquals(10, dashboard.pendingBookings());
        assertEquals(150, dashboard.completedBookings());
        assertEquals(20, dashboard.cancelledBookings());
        assertEquals(new BigDecimal("7500.00"), dashboard.totalRevenue());
        assertEquals(5, dashboard.pendingProviderVerifications());
    }
}
