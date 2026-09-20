package com.kh.serviceplatform.features.provider.application;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationRequest;
import com.kh.serviceplatform.features.provider.application.dto.ProviderApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Provider Applications", description = "Endpoints for customer to apply and manage their provider onboarding")
@RestController
@RequestMapping("/api/v1/provider-applications")
@RequiredArgsConstructor
public class ProviderApplicationController {

    private final ProviderApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit an application to become a provider")
    public ProviderApplicationResponse submitApplication(@Valid @RequestBody ProviderApplicationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return applicationService.submitApplication(currentUserId, request);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's latest provider application")
    public ProviderApplicationResponse getMyLatestApplication() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return applicationService.getMyLatestApplication(currentUserId);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update current user's pending provider application")
    public ProviderApplicationResponse updateMyPendingApplication(@Valid @RequestBody ProviderApplicationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return applicationService.updateMyPendingApplication(currentUserId, request);
    }

    @PostMapping("/me/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel current user's pending provider application")
    public ProviderApplicationResponse cancelMyApplication() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return applicationService.cancelMyApplication(currentUserId);
    }
}
