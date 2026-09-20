package com.kh.serviceplatform.features.customer;

import com.kh.serviceplatform.features.customer.dto.CustomerDashboardResponse;
import com.kh.serviceplatform.features.customer.dto.CustomerProfileResponse;
import com.kh.serviceplatform.features.customer.dto.UpdateCustomerProfileRequest;

import java.util.UUID;

public interface CustomerProfileService {

    CustomerProfileResponse getProfileByUserId(UUID userId);

    CustomerProfileResponse getProfileById(UUID id);

    CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request);

    CustomerDashboardResponse getCustomerDashboard(UUID userId);

    CustomerProfile createDefaultProfile(UUID userId);
}
