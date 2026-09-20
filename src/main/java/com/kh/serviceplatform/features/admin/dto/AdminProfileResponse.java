package com.kh.serviceplatform.features.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        String department,
        String position,
        String emergencyContact,
        Instant createdAt,
        Instant updatedAt
) {
}
