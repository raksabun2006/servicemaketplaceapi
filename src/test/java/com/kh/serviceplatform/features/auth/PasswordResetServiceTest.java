package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.email.EmailService;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.response.MessageResponse;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.features.auth.dto.ForgotPasswordRequest;
import com.kh.serviceplatform.features.auth.dto.ResetPasswordRequest;
import com.kh.serviceplatform.features.auth.enums.AuthProvider;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.provider.application.ProviderApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String GENERIC_MESSAGE =
            "If an account exists with this email, a password reset link has been sent.";
    private static final String INVALID_OR_EXPIRED_MESSAGE =
            "Password reset link is invalid or expired.";

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

    private User sampleUser;

    @BeforeEach
    void setUp() {
        authService.setTokenExpirationMinutes(15);
        authService.setRequestCooldownSeconds(60);
        authService.setFrontendUrl("https://servicemaketplacefront.vercel.app");
        authService.setResetPasswordPath("/reset-password");

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Sok Dara")
                .email("dara@example.com")
                .phone("012345678")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .passwordHash("currentHashedPassword")
                .build();
    }

    // ==========================================
    // FORGOT PASSWORD TESTS
    // ==========================================

    @Test
    void forgotPassword_withExistingActiveLocalUser_shouldGenerateToken_invalidatePreviousTokens_sendEmail_andReturnGenericMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals(GENERIC_MESSAGE, response.message());

        // Invalidate previous tokens
        verify(passwordResetTokenRepository).invalidateAllUnusedTokensByUser(eq(sampleUser), any(Instant.class));

        // Save new token in DB
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(sampleUser, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
        assertNull(savedToken.getUsedAt());

        // Send email with reset URL containing raw token
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(
                eq("dara@example.com"),
                eq("Sok Dara"),
                urlCaptor.capture(),
                eq(15)
        );

        String resetUrl = urlCaptor.getValue();
        assertTrue(resetUrl.startsWith("https://servicemaketplacefront.vercel.app/reset-password?token="));
        String rawToken = resetUrl.substring(resetUrl.indexOf("token=") + 6);
        assertFalse(rawToken.isBlank());

        // Hashing the raw token must produce the stored token_hash
        String computedHash = PasswordResetTokenUtils.hashToken(rawToken);
        assertEquals(savedToken.getTokenHash(), computedHash);
    }

    @Test
    void forgotPassword_whenEmailServiceThrowsMailException_shouldCatchAndMaintainGenericResponse() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Optional.empty());
        doThrow(new MailSendException("Couldn't connect to host, port: smtp.gmail.com, 587"))
                .when(emailService).sendPasswordResetEmail(eq("dara@example.com"), anyString(), anyString(), anyInt());

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals(GENERIC_MESSAGE, response.message());
    }

    @Test
    void forgotPassword_withNonExistingEmail_shouldNotSendEmail_andReturnGenericMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");

        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals(GENERIC_MESSAGE, response.message());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_withInactiveUser_shouldNotSendEmail_andReturnGenericMessage() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals(GENERIC_MESSAGE, response.message());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_withGoogleOnlyUser_shouldNotSendEmail_andReturnGenericMessage() {
        sampleUser.setAuthProvider(AuthProvider.GOOGLE);
        sampleUser.setGoogleSubject("google-sub-12345");
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals(GENERIC_MESSAGE, response.message());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_shouldNormalizeEmailBeforeLookup() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("   DARA@EXAMPLE.COM   ");

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertEquals(GENERIC_MESSAGE, response.message());
        verify(userRepository).findByEmailIgnoreCase("dara@example.com");
    }

    @Test
    void forgotPassword_shouldEnforceRateLimitingCooldown() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        PasswordResetToken recentToken = PasswordResetToken.builder()
                .user(sampleUser)
                .tokenHash("somehash")
                .createdAt(Instant.now().minusSeconds(30)) // created 30 seconds ago (cooldown is 60s)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Optional.of(recentToken));

        MessageResponse response = authService.forgotPassword(request);

        // Cooldown active: generic message returned, no email sent, no new token persisted
        assertEquals(GENERIC_MESSAGE, response.message());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_whenCooldownExpired_shouldAllowNewResetRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("dara@example.com");

        PasswordResetToken expiredCooldownToken = PasswordResetToken.builder()
                .user(sampleUser)
                .tokenHash("somehash")
                .createdAt(Instant.now().minusSeconds(65)) // created 65 seconds ago (cooldown 60s expired)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(userRepository.findByEmailIgnoreCase("dara@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Optional.of(expiredCooldownToken));

        MessageResponse response = authService.forgotPassword(request);

        assertEquals(GENERIC_MESSAGE, response.message());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("dara@example.com"), anyString(), anyString(), anyInt());
    }

    @Test
    void forgotPassword_withNullOrBlankEmail_shouldReturnGenericMessage() {
        MessageResponse nullResponse = authService.forgotPassword(null);
        assertEquals(GENERIC_MESSAGE, nullResponse.message());

        MessageResponse blankResponse = authService.forgotPassword(new ForgotPasswordRequest("   "));
        assertEquals(GENERIC_MESSAGE, blankResponse.message());

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    // ==========================================
    // RESET PASSWORD TESTS
    // ==========================================

    @Test
    void resetPassword_withValidToken_shouldEncodeNewPassword_setUsedAt_andSucceed() {
        String rawToken = "my-valid-secure-token-1234567890";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("newBcryptEncodedHash");

        MessageResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertEquals("Password has been reset successfully.", response.message());

        // Verify password hash updated on user
        assertEquals("newBcryptEncodedHash", sampleUser.getPasswordHash());
        verify(userRepository).save(sampleUser);

        // Verify token marked as used
        assertNotNull(resetToken.getUsedAt());
        verify(passwordResetTokenRepository).save(resetToken);

        // Verify all other tokens invalidated
        verify(passwordResetTokenRepository).invalidateAllUnusedTokensByUser(eq(sampleUser), any(Instant.class));
    }

    @Test
    void resetPassword_withExpiredToken_shouldThrowBadRequestException() {
        String rawToken = "expired-token-12345";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1))) // expired 1 minute ago
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withAlreadyUsedToken_shouldThrowBadRequestException() {
        String rawToken = "already-used-token-12345";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        PasswordResetToken usedToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .usedAt(Instant.now().minusSeconds(120)) // used 2 minutes ago
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(usedToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withInvalidTokenHash_shouldThrowBadRequestException() {
        String rawToken = "non-existent-token-12345";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withMismatchedPasswords_shouldThrowBadRequestException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "token-123",
                "Password@123",
                "DifferentPassword@456"
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals("Passwords do not match", ex.getReason());

        verify(passwordResetTokenRepository, never()).findByTokenHash(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withInactiveUser_shouldThrowBadRequestException() {
        String rawToken = "valid-token-for-inactive-user";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        sampleUser.setStatus(UserStatus.LOCKED);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withGoogleOnlyUser_shouldThrowBadRequestException() {
        String rawToken = "valid-token-for-google-user";
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        sampleUser.setAuthProvider(AuthProvider.GOOGLE);

        ResetPasswordRequest request = new ResetPasswordRequest(
                rawToken,
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_withBlankToken_shouldThrowBadRequestException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "   ",
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertEquals(INVALID_OR_EXPIRED_MESSAGE, ex.getReason());
    }
}
