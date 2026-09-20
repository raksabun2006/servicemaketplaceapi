package com.kh.serviceplatform.features.provider;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.provider.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Provider Management", description = "Endpoints for provider profiles, location discovery, availability, and verification")
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Get current provider profile", description = "Retrieve profile details of the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider profile found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public ProviderProfileResponse getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.getProfileByUserId(currentUserId);
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Create provider profile", description = "Initialize provider profile for the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ProviderProfileResponse createProfile(@Valid @RequestBody CreateProviderProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.createProfile(currentUserId, request);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Update current provider profile", description = "Update profile details of the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation or business rule violation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public ProviderProfileResponse updateMyProfile(@Valid @RequestBody UpdateProviderProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.updateProfile(currentUserId, request);
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Update provider availability", description = "Set availability status (AVAILABLE, BUSY, OFFLINE), working hours, and coverage radius",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability settings updated"),
            @ApiResponse(responseCode = "400", description = "Invalid radius or availability parameters")
    })
    public ProviderProfileResponse updateAvailability(@Valid @RequestBody UpdateAvailabilityRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.updateAvailability(currentUserId, request);
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Get provider dashboard metrics", description = "Retrieve aggregated performance metrics for the authenticated provider",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ProviderDashboardResponse getProviderDashboard() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.getProviderDashboard(currentUserId);
    }

    @PostMapping("/me/verification")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Submit provider verification", description = "Submit identity document and photo for admin verification",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or document provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires PROVIDER role")
    })
    public ProviderProfileResponse submitVerification(@Valid @RequestBody ProviderVerificationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return providerProfileService.submitVerification(currentUserId, request);
    }

    @GetMapping
    @Operation(summary = "List available providers", description = "Browse all active and available providers publicly, optionally filtered by available now")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of providers retrieved")
    })
    public Page<ProviderProfileResponse> getAvailableProviders(
            @RequestParam(required = false) Boolean availableNow,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return providerProfileService.getAvailableProviders(availableNow, pageable);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Search nearby providers", description = "Find active providers near specified latitude/longitude within radius in km")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nearby providers retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius")
    })
    public Page<NearbyProviderResponse> getNearbyProviders(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "10.0") Double radiusKm,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return providerProfileService.getNearbyProviders(latitude, longitude, radiusKm, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get provider by ID", description = "Retrieve public profile information of a provider by their profile ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider profile found"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ProviderProfileResponse getProfileById(@PathVariable UUID id) {
        return providerProfileService.getProfileById(id);
    }
}
