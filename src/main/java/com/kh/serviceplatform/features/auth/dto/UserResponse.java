package com.kh.serviceplatform.features.auth.dto;

import com.kh.serviceplatform.features.auth.enums.UserRole;
import lombok.Builder;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        UserRole role
) {
}