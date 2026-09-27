package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.email.EmailService;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.RegisterRequest;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.file.enums.FileType;
import com.kh.serviceplatform.features.provider.application.ProviderApplication;
import com.kh.serviceplatform.features.provider.application.ProviderApplicationRepository;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private ProviderApplicationRepository providerApplicationRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        lenient().when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token");
        lenient().when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Sok Dara",
                "dara@example.com",
                "012345678",
                UserRole.CUSTOMER,
                "Password@123",
                null, null, null, null, null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("dara@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Sok Dara")
                .email("dara@example.com")
                .phone("012345678")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(savedUser.getId(), "Sok Dara", "dara@example.com", "012345678", UserRole.CUSTOMER)
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals(UserRole.CUSTOMER, response.user().role());

        verify(userRepository).save(argThat(user -> user.getRole() == UserRole.CUSTOMER));
        verify(providerApplicationRepository, never()).save(any(ProviderApplication.class));
    }

    @Test
    void shouldRegisterProviderWithPendingApplication() {
        RegisterRequest request = new RegisterRequest(
                "Meas Sopheak",
                "meas.sopheak.provider@example.com",
                "0983456789",
                UserRole.PROVIDER,
                "Provider@12345",
                "Sopheak Electrical Service",
                "Professional electrical repair",
                5,
                "Phnom Penh",
                "Street 2004",
                "Phnom Penh",
                "Sen Sok",
                11.5564,
                104.9282,
                null,
                null
        );

        when(userRepository.existsByEmailIgnoreCase("meas.sopheak.provider@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0983456789")).thenReturn(false);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Meas Sopheak")
                .email("meas.sopheak.provider@example.com")
                .phone("0983456789")
                .role(UserRole.PROVIDER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(savedUser.getId(), "Meas Sopheak", "meas.sopheak.provider@example.com", "0983456789", UserRole.PROVIDER)
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(UserRole.PROVIDER, response.user().role());

        ArgumentCaptor<ProviderApplication> appCaptor = ArgumentCaptor.forClass(ProviderApplication.class);
        verify(providerApplicationRepository).save(appCaptor.capture());

        ProviderApplication capturedApp = appCaptor.getValue();
        assertEquals(savedUser, capturedApp.getUser());
        assertEquals("Sopheak Electrical Service", capturedApp.getBusinessName());
        assertEquals("Professional electrical repair", capturedApp.getBio());
        assertEquals(5, capturedApp.getExperienceYears());
        assertEquals("Phnom Penh", capturedApp.getServiceArea());
        assertEquals("0983456789", capturedApp.getPhone());
        assertEquals("Street 2004", capturedApp.getAddress());
        assertEquals("Phnom Penh", capturedApp.getCity());
        assertEquals("Sen Sok", capturedApp.getDistrict());
        assertEquals(11.5564, capturedApp.getLatitude());
        assertEquals(104.9282, capturedApp.getLongitude());
        assertEquals(ProviderApplicationStatus.PENDING, capturedApp.getApplicationStatus());
        assertNull(capturedApp.getIdentityDocumentFile());
        assertNull(capturedApp.getProfilePhotoFile());
    }

    @Test
    void shouldRegisterProviderWithFilesSuccessfully() {
        UUID docId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        RegisterRequest request = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "Vanna Aircon Service",
                "Aircon repairs",
                5,
                "Phnom Penh",
                "Street 271",
                "Phnom Penh",
                "Meanchey",
                11.5341,
                104.9060,
                docId,
                photoId
        );

        when(userRepository.existsByEmailIgnoreCase("vanna.sok@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);

        StoredFile identityDoc = StoredFile.builder()
                .id(docId)
                .fileType(FileType.PROVIDER_DOCUMENT)
                .build();
        StoredFile profilePhoto = StoredFile.builder()
                .id(photoId)
                .fileType(FileType.AVATAR)
                .build();

        when(fileRepository.findById(docId)).thenReturn(Optional.of(identityDoc));
        when(fileRepository.findById(photoId)).thenReturn(Optional.of(profilePhoto));

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Sok Vanna")
                .email("vanna.sok@example.com")
                .phone("012345678")
                .role(UserRole.PROVIDER)
                .status(UserStatus.ACTIVE)
                .avatarFile(profilePhoto)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(savedUser.getId(), "Sok Vanna", "vanna.sok@example.com", "012345678", UserRole.PROVIDER)
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(UserRole.PROVIDER, response.user().role());

        ArgumentCaptor<ProviderApplication> appCaptor = ArgumentCaptor.forClass(ProviderApplication.class);
        verify(providerApplicationRepository).save(appCaptor.capture());

        ProviderApplication capturedApp = appCaptor.getValue();
        assertEquals(identityDoc, capturedApp.getIdentityDocumentFile());
        assertEquals(profilePhoto, capturedApp.getProfilePhotoFile());
        assertEquals(ProviderApplicationStatus.PENDING, capturedApp.getApplicationStatus());
    }

    @Test
    void shouldRejectProviderRegistrationWhenFileNotFound() {
        UUID docId = UUID.randomUUID();

        RegisterRequest request = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "Vanna Aircon Service",
                "Aircon repairs",
                5,
                "Phnom Penh",
                null, null, null, null, null,
                docId,
                null
        );

        when(userRepository.existsByEmailIgnoreCase("vanna.sok@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);
        when(fileRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));
        verify(providerApplicationRepository, never()).save(any(ProviderApplication.class));
    }

    @Test
    void shouldRejectProviderWhenBusinessNameMissing() {
        RegisterRequest request = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "",
                "Bio",
                5,
                "Phnom Penh",
                null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("vanna.sok@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getReason().contains("Business name is required"));
        verify(userRepository, never()).save(any(User.class));
        verify(providerApplicationRepository, never()).save(any(ProviderApplication.class));
    }

    @Test
    void shouldRejectProviderWhenExperienceYearsMissingOrNegative() {
        RegisterRequest requestMissingExp = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "Vanna Aircon",
                "Bio",
                null,
                "Phnom Penh",
                null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("vanna.sok@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(requestMissingExp));

        RegisterRequest requestNegativeExp = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "Vanna Aircon",
                "Bio",
                -1,
                "Phnom Penh",
                null, null, null, null, null, null, null
        );

        assertThrows(BadRequestException.class, () -> authService.register(requestNegativeExp));
    }

    @Test
    void shouldRejectProviderWhenServiceAreaMissing() {
        RegisterRequest request = new RegisterRequest(
                "Sok Vanna",
                "vanna.sok@example.com",
                "012345678",
                UserRole.PROVIDER,
                "Password@123",
                "Vanna Aircon",
                "Bio",
                5,
                "   ",
                null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("vanna.sok@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("012345678")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getReason().contains("Service area is required"));
    }

    @Test
    void shouldRejectAdminRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Admin Wannabe",
                "admin@example.com",
                "011223344",
                UserRole.ADMIN,
                "Admin@123",
                null, null, null, null, null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Duplicate Email",
                "existing@example.com",
                "011223344",
                UserRole.CUSTOMER,
                "Password@123",
                null, null, null, null, null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void shouldRejectDuplicatePhone() {
        RegisterRequest request = new RegisterRequest(
                "Duplicate Phone",
                "unique@example.com",
                "099887766",
                UserRole.CUSTOMER,
                "Password@123",
                null, null, null, null, null, null, null, null, null, null, null
        );

        when(userRepository.existsByEmailIgnoreCase("unique@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("099887766")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }
}
