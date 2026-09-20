package com.kh.serviceplatform.features.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Admin platform analytics and summary metrics")
public record AdminDashboardResponse(
        long totalUsers,
        long totalCustomers,
        long totalProviders,
        long totalServices,
        long totalBookings,
        long pendingBookings,
        long completedBookings,
        long cancelledBookings,
        BigDecimal totalRevenue,
        long pendingProviderVerifications
) {
}
