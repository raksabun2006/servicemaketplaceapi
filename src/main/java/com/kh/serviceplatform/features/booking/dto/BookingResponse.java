package com.kh.serviceplatform.features.booking.dto;

import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Booking response payload")
public record BookingResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        UUID providerId,
        String providerBusinessName,
        String providerFullName,
        String providerPhone,
        UUID serviceId,
        String serviceName,
        UUID serviceRequestId,
        String serviceRequestTitle,
        UUID acceptedOfferId,
        BigDecimal price,
        BookingStatus status,
        String address,
        String city,
        LocalDate scheduledDate,
        String scheduledStartTime,
        String scheduledEndTime,
        Instant scheduledAt,
        String notes,
        String cancellationReason,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
        public BookingResponse(
                UUID id,
                UUID customerId,
                String customerName,
                String customerEmail,
                String customerPhone,
                UUID providerId,
                String providerBusinessName,
                String providerFullName,
                String providerPhone,
                UUID serviceId,
                String serviceName,
                BigDecimal price,
                BookingStatus status,
                String address,
                String city,
                Instant scheduledAt,
                String notes,
                String cancellationReason,
                String rejectionReason,
                Instant createdAt,
                Instant updatedAt
        ) {
                this(id, customerId, customerName, customerEmail, customerPhone, providerId, providerBusinessName, providerFullName, providerPhone, serviceId, serviceName, null, null, null, price, status, address, city, null, null, null, scheduledAt, notes, cancellationReason, rejectionReason, createdAt, updatedAt);
        }
}
