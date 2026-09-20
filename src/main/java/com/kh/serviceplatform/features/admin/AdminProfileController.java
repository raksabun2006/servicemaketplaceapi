package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.admin.dto.AdminProfileResponse;
import com.kh.serviceplatform.features.admin.dto.UpdateAdminProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Profile", description = "Admin profile management endpoints")
@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get current admin profile")
    public AdminProfileResponse getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return adminProfileService.getProfileByUserId(currentUserId);
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update current admin profile with avatar file upload")
    public AdminProfileResponse updateMyProfile(@Valid @ModelAttribute UpdateAdminProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return adminProfileService.updateProfile(currentUserId, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin profile by ID")
    public AdminProfileResponse getProfileById(@PathVariable UUID id) {
        return adminProfileService.getProfileById(id);
    }
}
