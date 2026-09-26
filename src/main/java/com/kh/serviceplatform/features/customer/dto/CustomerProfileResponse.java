package com.kh.serviceplatform.features.customer.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        String preferredLanguage,
        String preferredCurrency,
        String address,
        String city,
        String district,
        String postalCode,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
