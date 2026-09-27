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
import com.kh.serviceplatform.features.auth.enums.AuthProvider;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.customer.CustomerProfile;
import com.kh.serviceplatform.features.customer.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        String credentialToken = request != null ? request.resolveCredential() : null;
        if (credentialToken == null || credentialToken.isBlank()) {
            throw new BadRequestException("Google credential token is required");
        }

        // 1. Verify Google ID token and extract trusted claims
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verifyToken(credentialToken);

        if (!googleUserInfo.isEmailVerified()) {
            log.warn("Google login rejected: email is not verified by Google for sub: {}", googleUserInfo.getSubject());
            throw new UnauthorizedException("Google account email is not verified");
        }

        String googleSubject = googleUserInfo.getSubject();
        String googleEmail = googleUserInfo.getEmail().trim().toLowerCase();

        // 2. Check if user already exists by googleSubject
        Optional<User> userBySubject = userRepository.findByGoogleSubject(googleSubject);
        User user;

        if (userBySubject.isPresent()) {
            user = userBySubject.get();
            log.info("Found existing user by Google subject: {} (email: {})", googleSubject, user.getEmail());

            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Google login rejected: account is not active for email: {}", user.getEmail());
                throw new ForbiddenException("Your account is not active");
            }

            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
            }
            user.setLastLoginAt(Instant.now());
            user = userRepository.save(user);
        } else {
            // 3. If not found by googleSubject, check if an account exists with the verified email
            Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(googleEmail);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                log.info("Linking verified Google identity (sub: {}) to existing account: {} (id: {})",
                        googleSubject, user.getEmail(), user.getId());

                if (user.getStatus() != UserStatus.ACTIVE) {
                    log.warn("Google login rejected: existing account is not active for email: {}", user.getEmail());
                    throw new ForbiddenException("Your account is not active");
                }

                user.setGoogleSubject(googleSubject);
                user.setEmailVerified(true);
                user.setLastLoginAt(Instant.now());

                user = userRepository.save(user);
                log.info("Successfully linked Google identity to account: {}", user.getEmail());
            } else {
                // 4. Create new Google CUSTOMER user
                String fullName = googleUserInfo.getName();
                if (fullName == null || fullName.isBlank()) {
                    if (googleEmail.contains("@")) {
                        fullName = googleEmail.substring(0, googleEmail.indexOf('@'));
                    } else {
                        fullName = "Google User";
                    }
                }

                user = User.builder()
                        .fullName(fullName.trim())
                        .email(googleEmail)
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(UserRole.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .authProvider(AuthProvider.GOOGLE)
                        .googleSubject(googleSubject)
                        .emailVerified(true)
                        .phoneVerified(false)
                        .lastLoginAt(Instant.now())
                        .build();

                try {
                    user = userRepository.save(user);
                    log.info("Created new CUSTOMER user via Google auth: {} (id: {}, email: {})",
                            user.getFullName(), user.getId(), user.getEmail());

                    CustomerProfile customerProfile = CustomerProfile.builder()
                            .user(user)
                            .preferredLanguage("en")
                            .preferredCurrency("USD")
                            .build();

                    customerProfileRepository.save(customerProfile);
                } catch (DataIntegrityViolationException ex) {
                    log.warn("Concurrent registration detected for Google email '{}': {}", googleEmail, ex.getMessage());
                    user = userRepository.findByEmailIgnoreCase(googleEmail)
                            .orElseThrow(() -> new BadRequestException("User registration conflict for email: " + googleEmail));
                    if (user.getGoogleSubject() == null) {
                        user.setGoogleSubject(googleSubject);
                        user.setEmailVerified(true);
                        user.setLastLoginAt(Instant.now());
                        user = userRepository.save(user);
                    }
                }
            }
        }

        // 5. Generate application JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900,
                userMapper.toResponse(user)
        );
    }
}
