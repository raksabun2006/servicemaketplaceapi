package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.common.util.GeoUtils;
import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.GoogleLoginRequest;
import com.kh.serviceplatform.features.auth.dto.LoginRequest;
import com.kh.serviceplatform.features.auth.dto.RegisterRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProviderApplicationRepository providerApplicationRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;
    private final GoogleAuthService googleAuthService;
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
            // Effortless customer onboarding: location fields are completely optional
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
