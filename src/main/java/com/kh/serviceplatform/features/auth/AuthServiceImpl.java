package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.email.EmailService;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.response.MessageResponse;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.dto.*;
import com.kh.serviceplatform.features.auth.enums.AuthProvider;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.auth.google.GoogleAuthService;
import com.kh.serviceplatform.features.customer.CustomerProfile;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.provider.application.ProviderApplication;
import com.kh.serviceplatform.features.provider.application.ProviderApplicationRepository;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "If an account exists with this email, a password reset link has been sent.";
    private static final String INVALID_OR_EXPIRED_TOKEN_MESSAGE =
            "Password reset link is invalid or expired.";

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderApplicationRepository providerApplicationRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Setter
    @Value("${security.password-reset.token-expiration-minutes:15}")
    private int tokenExpirationMinutes = 15;

    @Setter
    @Value("${security.password-reset.request-cooldown-seconds:60}")
    private int requestCooldownSeconds = 60;

    @Setter
    @Value("${app.frontend-url:https://servicemaketplacefront.vercel.app}")
    private String frontendUrl = "https://servicemaketplacefront.vercel.app";

    @Setter
    @Value("${app.password-reset.path:/reset-password}")
    private String resetPasswordPath = "/reset-password";

    @Override
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        return googleAuthService.loginWithGoogle(request);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        // 1. Check duplicate email
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already registered");
        }

        // 2. Check duplicate phone (only if provided and non-blank)
        if (request.phone() != null &&
                !request.phone().isBlank() &&
                userRepository.existsByPhone(request.phone().trim())) {
            throw new BadRequestException("Phone number already registered");
        }

        // 3. Validate role (only CUSTOMER and PROVIDER are allowed)
        UserRole requestedRole = request.role() != null ? request.role() : UserRole.CUSTOMER;
        if (requestedRole != UserRole.CUSTOMER && requestedRole != UserRole.PROVIDER) {
            throw new BadRequestException("Registration is only permitted for CUSTOMER or PROVIDER roles.");
        }

        // 4. Validate provider-specific fields ONLY if registering as PROVIDER
        // Customers do NOT need to provide any business or location details!
        if (requestedRole == UserRole.PROVIDER) {
            if (request.businessName() == null || request.businessName().isBlank()) {
                throw new BadRequestException("Business name is required for provider registration");
            }
            if (request.experienceYears() == null) {
                throw new BadRequestException("Experience years is required for provider registration");
            }
            if (request.experienceYears() < 0) {
                throw new BadRequestException("Experience years cannot be negative");
            }
            if (request.serviceArea() == null || request.serviceArea().isBlank()) {
                throw new BadRequestException("Service area is required for provider registration");
            }
            if (request.latitude() != null && request.longitude() != null) {
                GeoUtils.validateCoordinates(request.latitude(), request.longitude());
            } else if (request.latitude() != null || request.longitude() != null) {
                throw new BadRequestException("Both latitude and longitude must be provided together");
            }
        }

        StoredFile identityDoc = null;
        if (request.identityDocumentFileId() != null) {
            identityDoc = fileRepository.findById(request.identityDocumentFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Identity document file not found with ID: " + request.identityDocumentFileId()));
        }

        StoredFile profilePhoto = null;
        if (request.profilePhotoFileId() != null) {
            profilePhoto = fileRepository.findById(request.profilePhotoFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile photo file not found with ID: " + request.profilePhotoFileId()));
        }

        // 5. Create User
        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(normalizePhone(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(requestedRole)
                .status(UserStatus.ACTIVE)
                .avatarFile(profilePhoto)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        user = userRepository.save(user);

        // 6. Handle role-specific profile creation
        if (requestedRole == UserRole.CUSTOMER) {
            CustomerProfile customerProfile = CustomerProfile.builder()
                    .user(user)
                    .preferredLanguage("en")
                    .preferredCurrency("USD")
                    .address(request.address() != null && !request.address().isBlank() ? request.address().trim() : null)
                    .city(request.city() != null && !request.city().isBlank() ? request.city().trim() : null)
                    .district(request.district() != null && !request.district().isBlank() ? request.district().trim() : null)
                    .build();

            customerProfileRepository.save(customerProfile);
            log.info("Customer registered successfully without requiring mandatory location: {}", user.getEmail());
        } else if (requestedRole == UserRole.PROVIDER) {
            ProviderApplication application = ProviderApplication.builder()
                    .user(user)
                    .businessName(request.businessName().trim())
                    .bio(request.bio() != null && !request.bio().isBlank() ? request.bio().trim() : null)
                    .experienceYears(request.experienceYears())
                    .serviceArea(request.serviceArea().trim())
                    .phone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : user.getPhone())
                    .address(request.address() != null && !request.address().isBlank() ? request.address().trim() : null)
                    .city(request.city() != null && !request.city().isBlank() ? request.city().trim() : null)
                    .district(request.district() != null && !request.district().isBlank() ? request.district().trim() : null)
                    .latitude(request.latitude())
                    .longitude(request.longitude())
                    .applicationStatus(ProviderApplicationStatus.PENDING)
                    .identityDocumentFile(identityDoc)
                    .profilePhotoFile(profilePhoto)
                    .build();

            providerApplicationRepository.save(application);
            log.info("Provider registered with pending application. Created user: {} and application for business: {}",
                    user.getEmail(), application.getBusinessName());
        }

        // 7. Generate tokens with the assigned role
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        // Check account status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Your account is not active");
        }

        // Update last login
        user.setLastLoginAt(java.time.Instant.now());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
        }

        String email = request.email().trim().toLowerCase();

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existing email");
            return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
        }

        User user = userOptional.get();

        // 1. Account status check: only ACTIVE users are permitted to receive reset emails
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Password reset requested for inactive account status: {} (userId: {})", user.getStatus(), user.getId());
            return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
        }

        // 2. Google-only account check: do not allow password reset for pure Google accounts
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            log.info("Password reset requested for Google-only account (userId: {})", user.getId());
            return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
        }

        Instant now = Instant.now();

        // 3. Rate limiting / Cooldown check: prevent rapid repeated reset requests
        Optional<PasswordResetToken> latestTokenOpt = passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (latestTokenOpt.isPresent()) {
            PasswordResetToken latestToken = latestTokenOpt.get();
            if (latestToken.getCreatedAt() != null) {
                Instant cooldownEnd = latestToken.getCreatedAt().plusSeconds(requestCooldownSeconds);
                if (now.isBefore(cooldownEnd)) {
                    log.warn("Password reset request rate-limited for user: {}", user.getId());
                    return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
                }
            }
        }

        // 4. Invalidate all previous unused tokens for this user
        passwordResetTokenRepository.invalidateAllUnusedTokensByUser(user, now);

        // 5. Generate cryptographically secure token and SHA-256 hash
        String rawToken = PasswordResetTokenUtils.generateRawToken();
        String tokenHash = PasswordResetTokenUtils.hashToken(rawToken);

        // 6. Save hashed token and expiration in database
        Instant expiresAt = now.plus(Duration.ofMinutes(tokenExpirationMinutes));
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // 7. Generate frontend reset URL (contains raw token in query parameter)
        String resetUrl = buildResetUrl(rawToken);

        // 8. Dispatch email via EmailService abstraction (never log rawToken or resetUrl)
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl, tokenExpirationMinutes);
        } catch (Exception ex) {
            log.error("Password reset email delivery failed for user {}: {}", user.getId(), ex.getMessage());
        }

        return MessageResponse.of(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new BadRequestException(INVALID_OR_EXPIRED_TOKEN_MESSAGE);
        }

        if (request.newPassword() == null || request.confirmPassword() == null ||
                !request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // 1. Hash the incoming raw token with SHA-256
        String tokenHash = PasswordResetTokenUtils.hashToken(request.token().trim());

        // 2. Find token by hash in database
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(INVALID_OR_EXPIRED_TOKEN_MESSAGE));

        // 3. Verify token validity (not expired, not already used)
        if (!resetToken.isValid()) {
            throw new BadRequestException(INVALID_OR_EXPIRED_TOKEN_MESSAGE);
        }

        // 4. Validate associated user
        User user = resetToken.getUser();
        if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new BadRequestException(INVALID_OR_EXPIRED_TOKEN_MESSAGE);
        }

        // 5. Update user password using BCrypt password encoder
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // 6. Invalidate reset token after successful reset
        Instant now = Instant.now();
        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);

        // Invalidate any other leftover tokens for this user
        passwordResetTokenRepository.invalidateAllUnusedTokensByUser(user, now);

        log.info("Password successfully reset for user: {}", user.getId());

        return MessageResponse.of("Password has been reset successfully.");
    }

    private String buildResetUrl(String rawToken) {
        String base = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.trim() : "http://localhost:3000";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = (resetPasswordPath != null && !resetPasswordPath.isBlank()) ? resetPasswordPath.trim() : "/reset-password";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private AuthResponse buildAuthResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900,
                mapper.toResponse(user)
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }
}
