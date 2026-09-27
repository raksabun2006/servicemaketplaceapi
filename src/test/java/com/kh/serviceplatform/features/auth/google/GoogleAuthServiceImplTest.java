package com.kh.serviceplatform.features.auth.google;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.UnauthorizedException;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserMapper;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.GoogleLoginRequest;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.AuthProvider;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.customer.CustomerProfile;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
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
class GoogleAuthServiceImplTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private GoogleAuthServiceImpl googleAuthService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_random_pwd");
        lenient().when(jwtService.generateAccessToken(any(User.class))).thenReturn("google_jwt_access");
        lenient().when(jwtService.generateRefreshToken(any(User.class))).thenReturn("google_jwt_refresh");
    }

    @Test
    void shouldThrowBadRequestWhenCredentialIsEmpty() {
        assertThrows(BadRequestException.class, () -> googleAuthService.loginWithGoogle(new GoogleLoginRequest("")));
        assertThrows(BadRequestException.class, () -> googleAuthService.loginWithGoogle(new GoogleLoginRequest(null)));
        assertThrows(BadRequestException.class, () -> googleAuthService.loginWithGoogle(null));
    }

    @Test
    void shouldThrowUnauthorizedWhenEmailIsNotVerifiedByGoogle() {
        GoogleUserInfo unverifiedInfo = GoogleUserInfo.builder()
                .subject("sub_123")
                .email("unverified@example.com")
                .emailVerified(false)
                .name("Unverified User")
                .build();

        when(googleTokenVerifier.verifyToken("dummy_token")).thenReturn(unverifiedInfo);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> googleAuthService.loginWithGoogle(new GoogleLoginRequest("dummy_token")));

        assertTrue(ex.getReason().contains("Google account email is not verified"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldLoginExistingUserByGoogleSubject() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .subject("google_sub_999")
                .email("existing@example.com")
                .emailVerified(true)
                .name("Existing User")
                .build();

        when(googleTokenVerifier.verifyToken("valid_token")).thenReturn(userInfo);

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Existing User")
                .email("existing@example.com")
                .googleSubject("google_sub_999")
                .authProvider(AuthProvider.GOOGLE)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        when(userRepository.findByGoogleSubject("google_sub_999")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(
                new UserResponse(existingUser.getId(), "Existing User", "existing@example.com", null, UserRole.CUSTOMER)
        );

        AuthResponse response = googleAuthService.loginWithGoogle(new GoogleLoginRequest("valid_token"));

        assertNotNull(response);
        assertEquals("google_jwt_access", response.accessToken());
        assertEquals("google_jwt_refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(existingUser.getEmail(), response.user().email());

        verify(userRepository).save(existingUser);
        verifyNoInteractions(customerProfileRepository);
    }

    @Test
    void shouldThrowForbiddenWhenExistingGoogleUserIsInactive() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .subject("google_sub_inactive")
                .email("inactive@example.com")
                .emailVerified(true)
                .name("Inactive User")
                .build();

        when(googleTokenVerifier.verifyToken("valid_token")).thenReturn(userInfo);

        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Inactive User")
                .email("inactive@example.com")
                .googleSubject("google_sub_inactive")
                .status(UserStatus.SUSPENDED)
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findByGoogleSubject("google_sub_inactive")).thenReturn(Optional.of(inactiveUser));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> googleAuthService.loginWithGoogle(new GoogleLoginRequest("valid_token")));

        assertTrue(ex.getReason().contains("Your account is not active"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLinkGoogleSubjectToExistingUserByEmail() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .subject("google_sub_link")
                .email("linkme@example.com")
                .emailVerified(true)
                .name("Link Me")
                .build();

        when(googleTokenVerifier.verifyToken("valid_token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSubject("google_sub_link")).thenReturn(Optional.empty());

        User localUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Local Account")
                .email("linkme@example.com")
                .authProvider(AuthProvider.LOCAL)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmailIgnoreCase("linkme@example.com")).thenReturn(Optional.of(localUser));
        when(userRepository.save(any(User.class))).thenReturn(localUser);
        when(userMapper.toResponse(localUser)).thenReturn(
                new UserResponse(localUser.getId(), "Local Account", "linkme@example.com", null, UserRole.CUSTOMER)
        );

        AuthResponse response = googleAuthService.loginWithGoogle(new GoogleLoginRequest("valid_token"));

        assertNotNull(response);
        assertEquals("google_jwt_access", response.accessToken());
        assertEquals("google_sub_link", localUser.getGoogleSubject());
        assertTrue(localUser.isEmailVerified());
        assertNotNull(localUser.getLastLoginAt());

        verify(userRepository).save(localUser);
    }

    @Test
    void shouldCreateNewCustomerAndProfileWhenUserDoesNotExist() {
        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .subject("google_sub_new")
                .email("newuser@gmail.com")
                .emailVerified(true)
                .name("Google New User")
                .build();

        when(googleTokenVerifier.verifyToken("valid_token")).thenReturn(userInfo);
        when(userRepository.findByGoogleSubject("google_sub_new")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("newuser@gmail.com")).thenReturn(Optional.empty());

        UUID newUserId = UUID.randomUUID();
        User createdUser = User.builder()
                .id(newUserId)
                .fullName("Google New User")
                .email("newuser@gmail.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.GOOGLE)
                .googleSubject("google_sub_new")
                .emailVerified(true)
                .phoneVerified(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(createdUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(newUserId, "Google New User", "newuser@gmail.com", null, UserRole.CUSTOMER)
        );

        AuthResponse response = googleAuthService.loginWithGoogle(new GoogleLoginRequest("valid_token"));

        assertNotNull(response);
        assertEquals("google_jwt_access", response.accessToken());
        assertEquals("Google New User", response.user().fullName());
        assertEquals("newuser@gmail.com", response.user().email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("newuser@gmail.com", capturedUser.getEmail());
        assertEquals("Google New User", capturedUser.getFullName());
        assertEquals(UserRole.CUSTOMER, capturedUser.getRole());
        assertEquals(UserStatus.ACTIVE, capturedUser.getStatus());
        assertEquals(AuthProvider.GOOGLE, capturedUser.getAuthProvider());
        assertEquals("google_sub_new", capturedUser.getGoogleSubject());
        assertTrue(capturedUser.isEmailVerified());
        assertFalse(capturedUser.isPhoneVerified());

        ArgumentCaptor<CustomerProfile> profileCaptor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(customerProfileRepository).save(profileCaptor.capture());
        CustomerProfile capturedProfile = profileCaptor.getValue();
        assertEquals(createdUser, capturedProfile.getUser());
        assertEquals("en", capturedProfile.getPreferredLanguage());
        assertEquals("USD", capturedProfile.getPreferredCurrency());
    }
}
