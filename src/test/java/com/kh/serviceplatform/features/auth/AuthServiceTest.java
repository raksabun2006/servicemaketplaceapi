package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.RegisterRequest;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
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
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper mapper;

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
        verify(providerProfileRepository, never()).save(any(ProviderProfile.class));
    }

    @Test
    void shouldRegisterProviderDirectly() {
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
                BigDecimal.valueOf(11.5564),
                BigDecimal.valueOf(104.9282),
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

        ArgumentCaptor<ProviderProfile> profileCaptor = ArgumentCaptor.forClass(ProviderProfile.class);
        verify(providerProfileRepository).save(profileCaptor.capture());

        ProviderProfile capturedProfile = profileCaptor.getValue();
        assertEquals("Sopheak Electrical Service", capturedProfile.getBusinessName());
        assertEquals(5, capturedProfile.getExperienceYears());
        assertEquals("Phnom Penh", capturedProfile.getServiceArea());
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
