package com.kh.serviceplatform.features.notification.dto;

import com.kh.serviceplatform.features.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Notification response payload")
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID referenceId,
        String referenceType,
        boolean read,
        Instant createdAt
) {
}
