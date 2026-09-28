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
        Double latitude,
        Double longitude,
        Instant createdAt,
        Instant updatedAt
) {
    public CustomerProfileResponse(
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
        this(id, userId, fullName, email, phone, avatarUrl, preferredLanguage, preferredCurrency, address, city, district, postalCode, notes, null, null, createdAt, updatedAt);
    }
}
