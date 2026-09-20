package com.kh.serviceplatform.features.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer dashboard statistics and overview")
public record CustomerDashboardResponse(
        long myRequestsCount,
        long openRequestsCount,
        long pendingOffersCount,
        long upcomingBookingsCount,
        long completedServicesCount,
        long cancelledRequestsCount,
        long favoriteProvidersCount,
        long unreadNotificationsCount
) {
}
