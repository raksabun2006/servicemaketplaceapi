package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.admin.dto.AdminProfileResponse;
import com.kh.serviceplatform.features.admin.dto.UpdateAdminProfileRequest;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.file.FileService;
import com.kh.serviceplatform.features.file.enums.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProfileServiceImpl implements AdminProfileService {

    private final AdminProfileRepository adminProfileRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final AdminProfileMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public AdminProfileResponse getProfileByUserId(UUID userId) {
        AdminProfile profile = adminProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        return mapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProfileResponse getProfileById(UUID id) {
        AdminProfile profile = adminProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found with ID: " + id));

        return mapper.toResponse(profile);
    }

    @Override
    public AdminProfileResponse updateProfile(UUID userId, UpdateAdminProfileRequest request) {
        AdminProfile profile = adminProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        User user = profile.getUser();

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.phone() != null) {
            String trimmedPhone = request.phone().trim();
            if (!trimmedPhone.isBlank() && !trimmedPhone.equals(user.getPhone())) {
                if (userRepository.existsByPhone(trimmedPhone)) {
                    throw new BadRequestException("Phone number already registered");
                }
                user.setPhone(trimmedPhone);
            }
        }

        if (request.avatarUploadFile() != null && !request.avatarUploadFile().isEmpty()) {
            fileService.uploadFile(request.avatarUploadFile(), FileType.AVATAR, userId);
        }

        userRepository.save(user);

        if (request.department() != null) {
            profile.setDepartment(request.department().trim());
        }
        if (request.position() != null) {
            profile.setPosition(request.position().trim());
        }
        if (request.emergencyContact() != null) {
            profile.setEmergencyContact(request.emergencyContact().trim());
        }

        AdminProfile saved = adminProfileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    @Override
    public AdminProfile createDefaultProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        AdminProfile profile = AdminProfile.builder()
                .user(user)
                .build();

        return adminProfileRepository.save(profile);
    }
}
