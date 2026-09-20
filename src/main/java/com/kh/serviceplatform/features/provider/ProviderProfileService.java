package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.features.provider.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProviderProfileService {

    ProviderProfileResponse getProfileByUserId(UUID userId);

    ProviderProfileResponse getProfileById(UUID id);

    ProviderProfileResponse createProfile(UUID userId, CreateProviderProfileRequest request);

    ProviderProfileResponse updateProfile(UUID userId, UpdateProviderProfileRequest request);

    ProviderProfileResponse updateAvailability(UUID userId, UpdateAvailabilityRequest request);

    ProviderProfileResponse submitVerification(UUID userId, ProviderVerificationRequest request);

    Page<ProviderProfileResponse> getAvailableProviders(Boolean availableNow, Pageable pageable);

    Page<NearbyProviderResponse> getNearbyProviders(Double latitude, Double longitude, Double radiusKm, Pageable pageable);

    ProviderDashboardResponse getProviderDashboard(UUID userId);

    ProviderProfile createDefaultProfile(UUID userId);
}
