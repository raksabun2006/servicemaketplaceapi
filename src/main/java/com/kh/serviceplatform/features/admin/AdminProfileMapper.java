package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.features.admin.dto.AdminProfileResponse;
import com.kh.serviceplatform.features.auth.User;
import org.springframework.stereotype.Component;

@Component
public class AdminProfileMapper {

    public AdminProfileResponse toResponse(AdminProfile profile) {
        if (profile == null) {
            return null;
        }

        User user = profile.getUser();

        return new AdminProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getAvatarUrl() : null,
                profile.getDepartment(),
                profile.getPosition(),
                profile.getEmergencyContact(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
