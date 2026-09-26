package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationRequest;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationReviewRequest;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderApplicationServiceTest {

    @Mock
    private ProviderApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ProviderApplicationMapper mapper;

    @InjectMocks
    private ProviderApplicationServiceImpl applicationService;

    private User customerUser;
    private User adminUser;
    private UUID customerId;
    private UUID adminId;
    private ProviderApplicationRequest validRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerUser = User.builder()
                .id(customerId)
                .email("customer@example.com")
                .fullName("Customer User")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .phone("+85512345678")
                .build();

        adminId = UUID.randomUUID();
        adminUser = User.builder()
                .id(adminId)
                .email("admin@example.com")
                .fullName("Admin User")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        validRequest = new ProviderApplicationRequest(
                "Dara Electrical Services",
                "Professional electrician with 5 years experience",
                5,
                "Phnom Penh",
                "+85512345678",
                "Street 2004",
                "Phnom Penh",
                "Sen Sok",
                11.5564,
                104.9282,
                null,
                null
        );
    }

    @Test
    void shouldSubmitApplicationSuccessfully() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));
        when(applicationRepository.existsByUserIdAndApplicationStatus(customerId, ProviderApplicationStatus.PENDING)).thenReturn(false);

        ProviderApplication savedApp = ProviderApplication.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .businessName("Dara Electrical Services")
                .experienceYears(5)
                .serviceArea("Phnom Penh")
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .build();

        when(applicationRepository.save(any(ProviderApplication.class))).thenReturn(savedApp);
        when(mapper.toResponse(savedApp)).thenReturn(
                new ProviderApplicationResponse(
                        savedApp.getId(),
                        new ProviderApplicationResponse.ApplicantDto(customerId, "Customer User", "customer@example.com", "+85512345678"),
                        "Dara Electrical Services",
                        "Bio",
                        5,
                        "Phnom Penh",
                        "+85512345678",
                        "Street 2004",
                        "Phnom Penh",
                        "Sen Sok",
                        11.5564,
                        104.9282,
                        ProviderApplicationStatus.PENDING,
                        null, null, null, null, null, null, null, null, null
                )
        );

        ProviderApplicationResponse response = applicationService.submitApplication(customerId, validRequest);

        assertNotNull(response);
        assertEquals(ProviderApplicationStatus.PENDING, response.applicationStatus());
        verify(applicationRepository).save(any(ProviderApplication.class));
    }

    @Test
    void shouldPreventDuplicatePendingApplication() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));
        when(applicationRepository.existsByUserIdAndApplicationStatus(customerId, ProviderApplicationStatus.PENDING)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                applicationService.submitApplication(customerId, validRequest)
        );

        verify(applicationRepository, never()).save(any(ProviderApplication.class));
    }

    @Test
    void shouldPreventAlreadyProviderFromApplying() {
        customerUser.setRole(UserRole.PROVIDER);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));

        assertThrows(BadRequestException.class, () ->
                applicationService.submitApplication(customerId, validRequest)
        );
    }

    @Test
    void shouldGetMyLatestApplicationByUserId() {
        ProviderApplication app = ProviderApplication.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .businessName("Dara Electrical Services")
                .experienceYears(5)
                .serviceArea("Phnom Penh")
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findByUserId(customerId)).thenReturn(Optional.of(app));
        when(mapper.toResponse(app)).thenReturn(
                new ProviderApplicationResponse(
                        app.getId(),
                        new ProviderApplicationResponse.ApplicantDto(customerId, "Customer User", "customer@example.com", "+85512345678"),
                        "Dara Electrical Services",
                        "Bio",
                        5,
                        "Phnom Penh",
                        "+85512345678",
                        "Street 2004",
                        "Phnom Penh",
                        "Sen Sok",
                        11.5564,
                        104.9282,
                        ProviderApplicationStatus.PENDING,
                        null, null, null, null, null, null, null, null, null
                )
        );

        ProviderApplicationResponse response = applicationService.getMyLatestApplication(customerId);

        assertNotNull(response);
        assertEquals("Dara Electrical Services", response.businessName());
        assertEquals(ProviderApplicationStatus.PENDING, response.applicationStatus());
        verify(applicationRepository).findByUserId(customerId);
    }

    @Test
    void shouldThrowNotFoundWhenNoApplicationForUser() {
        when(applicationRepository.findByUserId(customerId)).thenReturn(Optional.empty());
        when(applicationRepository.findTopByUserIdOrderByCreatedAtDesc(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.getMyLatestApplication(customerId)
        );
    }

    @Test
    void shouldCancelPendingApplication() {
        ProviderApplication app = ProviderApplication.builder()
                .id(UUID.randomUUID())
                .user(customerUser)
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findTopByUserIdOrderByCreatedAtDesc(customerId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(app)).thenReturn(app);

        applicationService.cancelMyApplication(customerId);

        assertEquals(ProviderApplicationStatus.CANCELLED, app.getApplicationStatus());
        verify(applicationRepository).save(app);
    }

    @Test
    void shouldApproveApplicationAndUpgradeUserRole() {
        UUID appId = UUID.randomUUID();
        ProviderApplication app = ProviderApplication.builder()
                .id(appId)
                .user(customerUser)
                .businessName("Dara Electrical Services")
                .experienceYears(5)
                .serviceArea("Phnom Penh")
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(providerProfileRepository.findByUserId(customerId)).thenReturn(Optional.empty());
        when(applicationRepository.save(app)).thenReturn(app);

        applicationService.approveApplication(appId, adminId);

        assertEquals(ProviderApplicationStatus.APPROVED, app.getApplicationStatus());
        assertEquals(adminUser, app.getReviewedBy());
        assertNotNull(app.getReviewedAt());
        assertEquals(UserRole.PROVIDER, customerUser.getRole());

        verify(userRepository).save(customerUser);
        verify(providerProfileRepository).save(argThat(ProviderProfile::isVerified));
        verify(applicationRepository).save(app);
    }

    @Test
    void shouldRejectApplicationWithReason() {
        UUID appId = UUID.randomUUID();
        ProviderApplication app = ProviderApplication.builder()
                .id(appId)
                .user(customerUser)
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(applicationRepository.save(app)).thenReturn(app);

        ProviderApplicationReviewRequest reviewRequest = new ProviderApplicationReviewRequest("Identity document is illegible");
        applicationService.rejectApplication(appId, adminId, reviewRequest);

        assertEquals(ProviderApplicationStatus.REJECTED, app.getApplicationStatus());
        assertEquals("Identity document is illegible", app.getRejectionReason());
        assertEquals(adminUser, app.getReviewedBy());
        assertEquals(UserRole.CUSTOMER, customerUser.getRole()); // Still CUSTOMER

        verify(applicationRepository).save(app);
        verify(providerProfileRepository, never()).save(any(ProviderProfile.class));
    }
}
