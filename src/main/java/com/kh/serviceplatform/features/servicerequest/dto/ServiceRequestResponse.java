package com.kh.serviceplatform.features.servicerequest.dto;

import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import com.kh.serviceplatform.features.servicerequest.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Service request / problem post response payload")
public record ServiceRequestResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerPhone,
        String customerEmail,
        UUID selectedProviderId,
        String selectedProviderBusinessName,
        String title,
        String description,
        ServiceCategory category,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        LocalDate preferredDate,
        String preferredTime,
        String address,
        String city,
        String district,
        Double latitude,
        Double longitude,
        Double distanceKm,
        boolean urgent,
        ServiceRequestStatus status,
        List<String> imageUrls,
        int offerCount,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
