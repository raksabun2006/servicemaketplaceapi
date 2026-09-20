package com.kh.serviceplatform.features.servicerequest.dto;

import com.kh.serviceplatform.features.servicerequest.enums.ServiceOfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Provider offer response payload")
public record ServiceRequestOfferResponse(
        UUID id,
        UUID serviceRequestId,
        UUID providerId,
        String providerBusinessName,
        String providerFullName,
        String providerPhone,
        Double providerAverageRating,
        Integer providerTotalReviews,
        BigDecimal proposedPrice,
        String message,
        String estimatedCompletionTime,
        ServiceOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
