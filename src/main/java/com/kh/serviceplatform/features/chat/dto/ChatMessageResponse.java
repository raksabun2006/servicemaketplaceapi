package com.kh.serviceplatform.features.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Chat message response payload")
public record ChatMessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String senderAvatarUrl,
        String message,
        boolean isMine,
        boolean read,
        Instant createdAt
) {
}
