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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Management", description = "Admin-only operations for users, providers, bookings, and dashboard analytics")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/users")
    @Operation(summary = "List all platform users", description = "Paginated list of all users with search and filter support",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public Page<UserResponse> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminManagementService.getUsers(search, role, status, pageable);
    }

    @GetMapping("/providers")
    @Operation(summary = "List all provider profiles", description = "Paginated list of all provider profiles with search and verification filters",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Providers retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public Page<ProviderProfileResponse> getProviders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProviderVerificationStatus verificationStatus,
            @RequestParam(required = false) Boolean isVerified,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminManagementService.getProviders(search, verificationStatus, isVerified, pageable);
    }

    @GetMapping("/providers/pending")
    @Operation(summary = "List pending provider verifications", description = "Paginated list of provider profiles pending admin verification",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending providers retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public Page<ProviderProfileResponse> getPendingProviders(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminManagementService.getPendingProviders(pageable);
    }

    @PutMapping("/providers/{providerId}/approve")
    @Operation(summary = "Approve provider verification", description = "Approve a provider profile and set verification status to VERIFIED",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider verified successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ProviderProfileResponse approveProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody(required = false) ProviderReviewActionRequest request
    ) {
        return adminManagementService.approveProvider(providerId, request);
    }

    @PutMapping("/providers/{providerId}/reject")
    @Operation(summary = "Reject provider verification", description = "Reject a provider profile verification with reason",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provider verification rejected"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ProviderProfileResponse rejectProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody(required = false) ProviderReviewActionRequest request
    ) {
        return adminManagementService.rejectProvider(providerId, request);
    }

    @GetMapping("/bookings")
    @Operation(summary = "List all bookings", description = "Paginated list of all system bookings with status and keyword filters",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public Page<BookingResponse> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminManagementService.getBookings(status, search, pageable);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform metrics dashboard", description = "Retrieve aggregated system-wide analytics and statistics",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public AdminDashboardResponse getDashboard() {
        return adminManagementService.getDashboard();
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get detailed marketplace analytics", description = "Retrieve comprehensive platform analytics including category and regional breakdown",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics data retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public AdminAnalyticsResponse getAnalytics() {
        return adminManagementService.getAnalytics();
    }
}
