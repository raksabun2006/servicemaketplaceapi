package com.kh.serviceplatform.features.customer;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.customer.dto.CustomerDashboardResponse;
import com.kh.serviceplatform.features.customer.dto.CustomerProfileResponse;
import com.kh.serviceplatform.features.customer.dto.UpdateCustomerProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer Management", description = "Endpoints for customer profiles and customer dashboard")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get current customer profile", description = "Retrieve profile details of authenticated customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CustomerProfileResponse getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return customerProfileService.getProfileByUserId(currentUserId);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Update current customer profile", description = "Update profile details of authenticated customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CustomerProfileResponse updateMyProfile(@Valid @RequestBody UpdateCustomerProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return customerProfileService.updateProfile(currentUserId, request);
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer dashboard overview", description = "Retrieve aggregated requests, bookings, and notifications statistics for current customer",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CustomerDashboardResponse getMyDashboard() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return customerProfileService.getCustomerDashboard(currentUserId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer profile by ID (Admin)", description = "Admin endpoint to view customer profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CustomerProfileResponse getProfileById(@PathVariable UUID id) {
        return customerProfileService.getProfileById(id);
    }
}
