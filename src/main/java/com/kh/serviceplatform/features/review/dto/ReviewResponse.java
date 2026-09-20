package com.kh.serviceplatform.features.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Review response payload")
public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID customerId,
        String customerName,
        String customerAvatarUrl,
        UUID providerId,
        String providerBusinessName,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
