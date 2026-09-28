package com.kh.serviceplatform.features.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to initiate or find a chat conversation")
public record CreateConversationRequest(
        @NotNull(message = "Provider ID is required")
        @Schema(description = "Provider profile UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID providerId,

        @Schema(description = "Associated service request ID if any", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID serviceRequestId,

        @Schema(description = "Associated booking ID if any", example = "123e4567-e89b-12d3-a456-426614174002")
        UUID bookingId,

        @Schema(description = "Initial message content to send immediately", example = "Hello, are you available tomorrow?")
        String initialMessage,

        @Schema(description = "Customer latitude coordinate", example = "11.5300")
        Double latitude,

        @Schema(description = "Customer longitude coordinate", example = "104.9000")
        Double longitude
) {
    public CreateConversationRequest(UUID providerId, UUID serviceRequestId, UUID bookingId, String initialMessage) {
        this(providerId, serviceRequestId, bookingId, initialMessage, null, null);
    }
}
