package com.kh.serviceplatform.features.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Admin marketplace analytics and aggregated metrics")
public record AdminAnalyticsResponse(
        long totalUsers,
        long totalCustomers,
        long totalProviders,
        long verifiedProviders,
        long pendingProviderVerifications,
        long openServiceRequests,
        long totalServiceRequests,
        long completedServices,
        long cancelledServices,
        long totalBookings,
        long pendingBookings,
        long completedBookings,
        long cancelledBookings,
        BigDecimal totalRevenue,
        double averageProviderRating,
        Map<String, Long> popularCategories,
        Map<String, Long> popularLocations
) {
}
