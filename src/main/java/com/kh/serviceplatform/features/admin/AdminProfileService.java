package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.features.admin.dto.AdminProfileResponse;
import com.kh.serviceplatform.features.admin.dto.UpdateAdminProfileRequest;

import java.util.UUID;

public interface AdminProfileService {

    AdminProfileResponse getProfileByUserId(UUID userId);

    AdminProfileResponse getProfileById(UUID id);

    AdminProfileResponse updateProfile(UUID userId, UpdateAdminProfileRequest request);

    AdminProfile createDefaultProfile(UUID userId);
}
