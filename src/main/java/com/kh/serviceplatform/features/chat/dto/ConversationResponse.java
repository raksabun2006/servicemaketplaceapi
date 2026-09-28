package com.kh.serviceplatform.features.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Conversation details with latest message summary and participant info")
public record ConversationResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerAvatarUrl,
        UUID providerId,
        String providerBusinessName,
        String providerFullName,
        String providerAvatarUrl,
        UUID serviceRequestId,
        String serviceRequestTitle,
        UUID bookingId,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount,
        Double distanceKm,
        Instant createdAt,
        Instant updatedAt
) {
    public ConversationResponse(
            UUID id,
            UUID customerId,
            String customerName,
            String customerAvatarUrl,
            UUID providerId,
            String providerBusinessName,
            String providerFullName,
            String providerAvatarUrl,
            UUID serviceRequestId,
            String serviceRequestTitle,
            UUID bookingId,
            String lastMessagePreview,
            Instant lastMessageAt,
            long unreadCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, customerId, customerName, customerAvatarUrl, providerId, providerBusinessName,
                providerFullName, providerAvatarUrl, serviceRequestId, serviceRequestTitle,
                bookingId, lastMessagePreview, lastMessageAt, unreadCount, null, createdAt, updatedAt);
    }
}
