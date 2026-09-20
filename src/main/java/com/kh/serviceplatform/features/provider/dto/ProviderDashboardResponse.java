package com.kh.serviceplatform.features.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Provider dashboard statistics and metrics")
public record ProviderDashboardResponse(
        long newNearbyRequestsCount,
        long pendingOffersCount,
        long acceptedServicesCount,
        long todayBookingsCount,
        long completedServicesCount,
        long cancelledServicesCount,
        double averageRating,
        int totalReviews
) {
}
