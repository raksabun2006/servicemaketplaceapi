package com.kh.serviceplatform.features.admin;

import com.kh.serviceplatform.features.admin.dto.AdminAnalyticsResponse;
import com.kh.serviceplatform.features.admin.dto.AdminDashboardResponse;
import com.kh.serviceplatform.features.admin.dto.ProviderReviewActionRequest;
import com.kh.serviceplatform.features.auth.dto.UserResponse;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import com.kh.serviceplatform.features.auth.enums.UserStatus;
import com.kh.serviceplatform.features.booking.dto.BookingResponse;
import com.kh.serviceplatform.features.booking.enums.BookingStatus;
import com.kh.serviceplatform.features.provider.dto.ProviderProfileResponse;
import com.kh.serviceplatform.features.provider.enums.ProviderVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminManagementService {

    Page<UserResponse> getUsers(String search, UserRole role, UserStatus status, Pageable pageable);

    Page<ProviderProfileResponse> getProviders(String search, ProviderVerificationStatus verificationStatus, Boolean isVerified, Pageable pageable);

    Page<ProviderProfileResponse> getPendingProviders(Pageable pageable);

    ProviderProfileResponse approveProvider(UUID providerId, ProviderReviewActionRequest request);

    ProviderProfileResponse rejectProvider(UUID providerId, ProviderReviewActionRequest request);

    Page<BookingResponse> getBookings(BookingStatus status, String search, Pageable pageable);

    AdminDashboardResponse getDashboard();

    AdminAnalyticsResponse getAnalytics();
}
