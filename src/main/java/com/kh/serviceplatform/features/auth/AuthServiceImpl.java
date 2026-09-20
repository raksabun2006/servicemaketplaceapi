package com.kh.serviceplatform.features.auth;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.security.JwtService;
import com.kh.serviceplatform.features.auth.dto.AuthResponse;
import com.kh.serviceplatform.features.auth.dto.LoginRequest;
import com.kh.serviceplatform.features.auth.dto.RegisterRequest;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.file.enums.FileType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.enums.AvailabilityStatus;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
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
    private final ProviderProfileRepository providerProfileRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;

    @Override
    public AuthResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        // 1. Check duplicate email
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already registered");
        }

        // 2. Check duplicate phone
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

        // 4. Create User directly with the requested role
        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(normalizePhone(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(requestedRole)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        user = userRepository.save(user);

        // 5. If registering as PROVIDER, directly create ProviderProfile
        if (requestedRole == UserRole.PROVIDER) {
            String businessName = (request.businessName() != null && !request.businessName().isBlank())
                    ? request.businessName().trim()
                    : user.getFullName();

            String serviceArea = (request.serviceArea() != null && !request.serviceArea().isBlank())
                    ? request.serviceArea().trim()
                    : "General";

            Integer experienceYears = request.experienceYears() != null ? request.experienceYears() : 0;

            StoredFile profilePhoto = null;
            if (request.profilePhotoFileId() != null) {
                profilePhoto = fileRepository.findById(request.profilePhotoFileId())
                        .filter(f -> f.getFileType() == FileType.AVATAR)
                        .orElse(null);
                if (profilePhoto != null) {
                    user.setAvatarFile(profilePhoto);
                    userRepository.save(user);
                }
            }

            StoredFile identityDoc = null;
            if (request.identityDocumentFileId() != null) {
                identityDoc = fileRepository.findById(request.identityDocumentFileId())
                        .filter(f -> f.getFileType() == FileType.PROVIDER_DOCUMENT)
                        .orElse(null);
            }

            ProviderProfile profile = ProviderProfile.builder()
                    .user(user)
                    .businessName(businessName)
                    .bio(request.bio() != null ? request.bio().trim() : null)
                    .experienceYears(experienceYears)
                    .serviceArea(serviceArea)
                    .address(request.address() != null ? request.address().trim() : null)
                    .city(request.city() != null ? request.city().trim() : null)
                    .district(request.district() != null ? request.district().trim() : null)
                    .latitude(request.latitude() != null ? request.latitude().doubleValue() : null)
                    .longitude(request.longitude() != null ? request.longitude().doubleValue() : null)
                    .profilePhotoFile(profilePhoto)
                    .identityDocumentFile(identityDoc)
                    .isAvailable(true)
                    .availabilityStatus(AvailabilityStatus.AVAILABLE)
                    .serviceRadiusKm(10.0)
                    .isVerified(false)
                    .verificationStatus(ProviderVerificationStatus.UNVERIFIED)
                    .averageRating(0.0)
                    .totalReviews(0)
                    .completedServices(0)
                    .build();

            providerProfileRepository.save(profile);
            log.info("Provider registered directly. Created user: {} and profile for business: {}",
                    user.getEmail(), businessName);
        }

        // 6. Generate tokens with the assigned role
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
