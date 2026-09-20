package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.file.enums.FileType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationRequest;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationReviewRequest;
import com.kh.serviceplatform.features.provider.application.enums.ProviderApplicationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProviderApplicationServiceImpl implements ProviderApplicationService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "businessName", "experienceYears", "serviceArea", "applicationStatus",
            "createdAt", "updatedAt", "reviewedAt"
    );

    private final ProviderApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final FileRepository fileRepository;
    private final ProviderApplicationMapper mapper;

    @Override
    public ProviderApplicationResponse submitApplication(UUID userId, ProviderApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Eligibility validation
        if (user.getRole() == UserRole.PROVIDER) {
            throw new BadRequestException("User is already a registered provider");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Admins cannot apply to become providers");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active");
        }

        // Prevent duplicate pending applications
        if (applicationRepository.existsByUserIdAndApplicationStatus(userId, ProviderApplicationStatus.PENDING)) {
            throw new BadRequestException("You already have an application under review");
        }

        StoredFile identityDoc = validateAndGetFile(request.identityDocumentFileId(), userId, FileType.PROVIDER_DOCUMENT, "Identity document");
        StoredFile profilePhoto = validateAndGetFile(request.profilePhotoFileId(), userId, FileType.AVATAR, "Profile photo");

        ProviderApplication application = ProviderApplication.builder()
                .user(user)
                .businessName(request.businessName().trim())
                .bio(request.bio() != null ? request.bio().trim() : null)
                .experienceYears(request.experienceYears())
                .serviceArea(request.serviceArea().trim())
                .phone(request.phone() != null ? request.phone().trim() : user.getPhone())
                .address(request.address() != null ? request.address().trim() : null)
                .city(request.city() != null ? request.city().trim() : null)
                .district(request.district() != null ? request.district().trim() : null)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .applicationStatus(ProviderApplicationStatus.PENDING)
                .identityDocumentFile(identityDoc)
                .profilePhotoFile(profilePhoto)
                .build();

        ProviderApplication saved = applicationRepository.save(application);
        log.info("Provider application submitted. ApplicationId: {}, UserId: {}", saved.getId(), userId);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderApplicationResponse getMyLatestApplication(UUID userId) {
        ProviderApplication application = applicationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider application found for current user"));

        return mapper.toResponse(application);
    }

    @Override
    public ProviderApplicationResponse updateMyPendingApplication(UUID userId, ProviderApplicationRequest request) {
        ProviderApplication application = applicationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider application found for current user"));

        if (application.getApplicationStatus() != ProviderApplicationStatus.PENDING) {
            throw new BadRequestException("Only applications in PENDING status can be updated");
        }

        StoredFile identityDoc = validateAndGetFile(request.identityDocumentFileId(), userId, FileType.PROVIDER_DOCUMENT, "Identity document");
        StoredFile profilePhoto = validateAndGetFile(request.profilePhotoFileId(), userId, FileType.AVATAR, "Profile photo");

        application.setBusinessName(request.businessName().trim());
        application.setBio(request.bio() != null ? request.bio().trim() : null);
        application.setExperienceYears(request.experienceYears());
        application.setServiceArea(request.serviceArea().trim());
        if (request.phone() != null) {
            application.setPhone(request.phone().trim());
        }
        application.setAddress(request.address() != null ? request.address().trim() : null);
        application.setCity(request.city() != null ? request.city().trim() : null);
        application.setDistrict(request.district() != null ? request.district().trim() : null);
        application.setLatitude(request.latitude());
        application.setLongitude(request.longitude());

        if (identityDoc != null) {
            application.setIdentityDocumentFile(identityDoc);
        }
        if (profilePhoto != null) {
            application.setProfilePhotoFile(profilePhoto);
        }

        ProviderApplication saved = applicationRepository.save(application);
        log.info("Provider application updated. ApplicationId: {}, UserId: {}", saved.getId(), userId);

        return mapper.toResponse(saved);
    }

    @Override
    public ProviderApplicationResponse cancelMyApplication(UUID userId) {
        ProviderApplication application = applicationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider application found for current user"));

        if (application.getApplicationStatus() != ProviderApplicationStatus.PENDING) {
            throw new BadRequestException("Only applications in PENDING status can be cancelled");
        }

        application.setApplicationStatus(ProviderApplicationStatus.CANCELLED);
        ProviderApplication saved = applicationRepository.save(application);
        log.info("Provider application cancelled. ApplicationId: {}, UserId: {}", saved.getId(), userId);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProviderApplicationResponse> getApplications(ProviderApplicationStatus status, String search, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);

        Specification<ProviderApplication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("applicationStatus"), status));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Join<ProviderApplication, User> userJoin = root.join("user", JoinType.LEFT);

                Predicate businessNameMatch = cb.like(cb.lower(root.get("businessName")), searchPattern);
                Predicate fullNameMatch = cb.like(cb.lower(userJoin.get("fullName")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(userJoin.get("email")), searchPattern);

                predicates.add(cb.or(businessNameMatch, fullNameMatch, emailMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return applicationRepository.findAll(spec, safePageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderApplicationResponse getApplicationById(UUID id) {
        ProviderApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider application not found with ID: " + id));

        return mapper.toResponse(application);
    }

    @Override
    public ProviderApplicationResponse approveApplication(UUID id, UUID adminId) {
        ProviderApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider application not found with ID: " + id));

        if (application.getApplicationStatus() != ProviderApplicationStatus.PENDING) {
            throw new BadRequestException("Application cannot be approved because current status is " + application.getApplicationStatus());
        }

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with ID: " + adminId));

        User applicant = application.getUser();

        // 1. Update user role
        applicant.setRole(UserRole.PROVIDER);
        if (application.getProfilePhotoFile() != null) {
            applicant.setAvatarFile(application.getProfilePhotoFile());
        }
        userRepository.save(applicant);

        // 2. Create or update ProviderProfile
        ProviderProfile profile = providerProfileRepository.findByUserId(applicant.getId())
                .orElseGet(() -> ProviderProfile.builder().user(applicant).build());

        profile.setBusinessName(application.getBusinessName());
        profile.setBio(application.getBio());
        profile.setExperienceYears(application.getExperienceYears());
        profile.setServiceArea(application.getServiceArea());
        profile.setLatitude(application.getLatitude());
        profile.setLongitude(application.getLongitude());
        profile.setVerified(true);
        profile.setAvailable(true);
        if (profile.getAverageRating() == null) {
            profile.setAverageRating(0.0);
        }
        if (profile.getTotalReviews() == null) {
            profile.setTotalReviews(0);
        }

        providerProfileRepository.save(profile);

        // 3. Update application status
        application.setApplicationStatus(ProviderApplicationStatus.APPROVED);
        application.setReviewedBy(adminUser);
        application.setReviewedAt(Instant.now());
        application.setRejectionReason(null);

        ProviderApplication saved = applicationRepository.save(application);
        log.info("Provider application approved. ApplicationId: {}, ApplicantId: {}, AdminId: {}", id, applicant.getId(), adminId);

        return mapper.toResponse(saved);
    }

    @Override
    public ProviderApplicationResponse rejectApplication(UUID id, UUID adminId, ProviderApplicationReviewRequest request) {
        ProviderApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider application not found with ID: " + id));

        if (application.getApplicationStatus() != ProviderApplicationStatus.PENDING) {
            throw new BadRequestException("Application cannot be rejected because current status is " + application.getApplicationStatus());
        }

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with ID: " + adminId));

        application.setApplicationStatus(ProviderApplicationStatus.REJECTED);
        application.setRejectionReason(request.reason().trim());
        application.setReviewedBy(adminUser);
        application.setReviewedAt(Instant.now());

        ProviderApplication saved = applicationRepository.save(application);
        log.info("Provider application rejected. ApplicationId: {}, ApplicantId: {}, AdminId: {}",
                id, application.getUser().getId(), adminId, request.reason());

        return mapper.toResponse(saved);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .toList();

        Sort validSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), validSort);
    }

    private StoredFile validateAndGetFile(UUID fileId, UUID currentUserId, FileType expectedType, String fieldName) {
        if (fileId == null) {
            return null;
        }

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException(fieldName + " not found with ID: " + fileId));

        if (!file.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not own the uploaded " + fieldName);
        }

        if (file.getFileType() != expectedType) {
            throw new BadRequestException(fieldName + " must be of type " + expectedType + " but was " + file.getFileType());
        }

        return file;
    }
}
